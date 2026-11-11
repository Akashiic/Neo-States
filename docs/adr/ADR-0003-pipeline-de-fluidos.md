# ADR-0003: API e pipeline de fluidos (altura, fluxo e estado)

* **Estado**: preliminar (Fase 0). É um desenho de interface; nenhum código foi escrito.
* **Contexto de leitura**: [DELTAS.md](../DELTAS.md), [ADR-0001](ADR-0001-armazenamento-waterlogging.md).

## Decisão

O Neo States expõe **uma** definição de "fluido nesta posição" e faz todas as leituras do jogo passarem por ela. Três
peças:

1. `FluidView`: o estado de fluido de uma célula, venha ele de um bloco líquido ou da camada de waterlogging.
2. `FluidPipeline`: uma cadeia de estágios que calcula **altura** e **fluxo** a partir do `FluidView`. O estágio base
   reproduz o vanilla; estágios extras (ondas, esteira) entram depois sem tocar no núcleo.
3. As **costuras**: os poucos mixins que desviam as leituras do vanilla para o pipeline.

## Por que isso resolve os três pedidos do item 4.1

| Pedido | Como |
|---|---|
| Nado em bloco waterlogged sem dessincronizar Player Backport e YesSteveModel | `Entity.isInWater()` e `isInsideOfMaterial()` passam pelas costuras; os testes de material que o Player Backport faz por conta própria passam a chamar a API |
| TileEntities intactas | a camada só guarda o bit ([ADR-0001](ADR-0001-armazenamento-waterlogging.md), invariante da Camada C) |
| Ondas e dinâmica futura sem desmontar o núcleo | altura e fluxo saem de um pipeline com estágios; quem lê não sabe quantos estágios há |

## 1. `FluidView`

Leitura imutável, sem alocação (um `int` empacotado ou campos de um objeto reutilizado por thread):

| Campo | Valores | Origem |
|---|---|---|
| tipo | vazio, água, lava | material do bloco, ou a camada |
| nível | 0..8 (`amount` moderno; 8 = fonte ou queda) | `8 − decay` do metadata; 8 para waterlogged |
| em queda | sim / não | metadata `>= 8` |
| hospedado | sim / não | veio da camada (há outro bloco na célula) |

Resolução de uma célula, na ordem: bloco com `Material.water` ou `Material.lava` → estado pelo metadata; senão, bit da
camada ligado → água fonte hospedada; senão vazio. O bloco de coluna de bolhas do Et Futurum (`Material.water`, não é
`BlockLiquid`, metadata 0 ou 8) cai no primeiro caso como água cheia.

Acesso por `IBlockAccess`: `World` e `ChunkCache` pela seção; `WorldSlice` do Angelica pelo mixin de acesso do
[ADR-0002](ADR-0002-interceptacao-visual.md).

## 2. Três alturas, não uma

O 1.7.10 tem cinco fórmulas de altura espalhadas (seção 3 de [VANILLA-1.7.10.md](../VANILLA-1.7.10.md)). O pipeline
separa três conceitos, porque eles têm requisitos diferentes:

| Altura | Quem usa | Requisito | Pode deformar? |
|---|---|---|---|
| **Lógica** | simulação de fluxo, regra das fontes | inteira em nonos; idêntica em cliente e servidor | nunca |
| **De interação** | corpo e olhos de entidades, anzol, barco, câmera | determinística a partir de (mundo, posição, tick); **igual nos dois lados** | só por estágio determinístico e sincronizado |
| **De render** | malha (por canto), partículas | só cliente | livre |

No estágio base as três valem `nível / 9` (ou `1` com o mesmo fluido em cima). Esta é a altura do 1.21.1 e também a
que o render do 1.7.10 e o `ModernBobber` do Kunftig já usam, então **ligar o pipeline não muda a pesca**.

A separação existe para o futuro: uma onda senoidal na malha é altura de render; se um dia ela empurrar o anzol, vira
altura de interação e precisa ser função do `totalWorldTime` e da posição, calculada igual no servidor.

## 3. Estágios

```java
public interface FluidStage {
    /** Altura da superfície na célula, em blocos, [0, 1]. */
    float height(FluidAccess access, int x, int y, int z, FluidView self, HeightKind kind, float previous);

    /** Soma o fluxo desta célula em {@code out}. */
    void flow(FluidAccess access, int x, int y, int z, FluidView self, MutableVec3 out);
}
```

* Estágio 0, sempre presente: equivalente ao vanilla. `height` devolve `nível/9`; `flow` chama o
  `BlockLiquid.getFlowVector` original (as leituras de vizinho dele já passam pelas costuras, então o vizinho
  waterlogged conta: delta B-6).
* Registro: `FluidPipeline.register(prioridade, estágio, lados)`. Lista congelada no `postInit`, sem alocação por
  consulta.
* Um estágio só pode alterar a altura de interação se declarar que é determinístico; o registro recusa o contrário.
* Altura de canto do render: `cornerHeight(access, x, y, z, canto)` é outro método do pipeline; o estágio base
  reproduz `RenderBlocks.getLiquidHeight`.

## 4. Costuras

Onde o vanilla passa a consultar o pipeline:

| Vanilla | Costura | Usa |
|---|---|---|
| `BlockLiquid.getFlowVector` (privado) | `@WrapMethod`: entra no pipeline; o estágio 0 chama o original | fluxo |
| `RenderBlocks.getLiquidHeight` | `@ModifyReturnValue` → `cornerHeight` | altura de render |
| `World.handleMaterialAcceleration` | `@WrapOperation` em `getBlock` / `getBlockMetadata`; teste de altura real (delta A-5) | altura de interação + fluxo |
| `Entity.isInsideOfMaterial` | `@WrapOperation` em `getBlock`; altura real (delta A-6) | altura de interação |
| `ActiveRenderInfo.getBlockAtEntityViewpoint` | `@WrapOperation` em `getBlock` / `getBlockMetadata` (delta A-7) | altura de interação |
| `World.isAnyLiquid`, `isMaterialInBB`, `isAABBInMaterial` | `@WrapOperation` em `getBlock` | tipo |
| `BlockLiquid.getEffectiveFlowDecay`, `func_149804_e`, `shouldSideBeRendered`, `isBlockSolid` | `@WrapOperation` em `getBlock` / `getBlockMetadata` | tipo e nível |

`BlockLiquid.getLiquidHeightPercent(int)` é estático e não recebe posição: fica como está (é a tabela da altura
lógica). Quem precisa de altura com posição chama o pipeline.

Cada costura é falha-fechada: `require = 1`, e um teste de impressão digital no build (no estilo do Player Backport)
confere o alvo contra as classes reais do cliente **e** do `server.jar` do Crucible.

## 5. API para os outros mods

Pacote `api`, estável e pequeno, consumível por reflexão (sem dependência de compilação):

```java
public final class NeoStates {
    public static int apiVersion();

    /** Estado de fluido da célula (bloco líquido ou camada). */
    public static FluidView fluidAt(IBlockAccess access, int x, int y, int z);

    /** Altura de interação da superfície, em blocos absolutos, ou NaN sem fluido. */
    public static double surfaceY(IBlockAccess access, int x, int y, int z);

    public static boolean isWaterAt(IBlockAccess access, int x, int y, int z);

    /** Liga ou desliga a água hospedada. Não valida o bloco. Só servidor. */
    public static boolean setWaterlogged(World world, int x, int y, int z, boolean water);

    public static boolean canHoldWater(Block block, int metadata);
}
```

Mais um registro de "quem pode conter água" (classe, bloco, bloco + metadata), preenchido por padrão com as classes
vanilla e aberto por configuração e por `FMLInterModComms`.

Consumidores previstos, todos em outros repositórios (mudanças a combinar):

| Mod | Hoje | Passa a chamar |
|---|---|---|
| Akashic Player Backport, `WaterPoseConditions`, `SwimmingVerticalHooks` | `getBlock().getMaterial() == Material.water` + `getLiquidHeightPercent` | `isWaterAt` / `surfaceY`, com a regra atual como reserva |
| Kunftig, `kunftig$fluidSurface`, `OpenWater` | material + `ModernBobber.surfaceHeight(metadata, …)` | `surfaceY`, com a regra atual como reserva |
| VitaGenesis, `Dripstone` e afins | só coloca em ar | `setWaterlogged` na geração |
| AkashicFishing | não lê altura | nada |

## 6. Telemetria

Antes de ligar qualquer estágio além do base, e em toda versão:

* **Pesca**: por anzol, a superfície do `ModernBobber` contra `NeoStates.surfaceY`; alerta com diferença `> 1e-6`.
* **Nado**: por jogador, `isInWater()` e `isInsideOfMaterial(water)` contra a pose do Player Backport; alerta quando a
  pose é `SWIMMING` com `isInWater() == false` por mais de N ticks.
* **Empurrão**: histograma de `|Δmotion|` por tick dentro d'água, para comparar com o Et Futurum ligado e desligado.
* Tudo atrás de `-Dneostates.telemetry=true`, desligado por padrão, fora do jar de release se for código de sonda.

## O que este ADR não decide

* O algoritmo de ondas, a esteira e qualquer dinâmica nova.
* Fluidos que não são água (lava hospedada, fluidos do Forge): o `FluidView` tem o campo de tipo, a camada v1 só
  guarda água.
* **[A VERIFICAR]** custo do `@WrapMethod` em `getFlowVector` no caminho de render (chamado por face de topo).
* **[A VERIFICAR]** convivência do `@WrapMethod` com o `@Invoker` e o `@Redirect` que o Angelica já tem nesse método e
  em `getFlowDirection`.
