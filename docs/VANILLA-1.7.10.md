# Vanilla 1.7.10: como a água funciona hoje

Referência do comportamento que o Neo States vai interceptar. Tudo aqui foi lido no código decompilado do
Minecraft 1.7.10 com os patches do Forge 10.13.4.1614 (RetroFuturaGradle, MCP `stable_12`), não de memória. As linhas
citadas são as desse decompilado (`build/rfg/minecraft-src/java`, tarefa `decompressDecompiledSources`); como reproduzir
está em [FASE-0.md](FASE-0.md#base-de-evidências).

O que não foi lido em código leva a marca **[A VERIFICAR]**.

## 1. Mapa de classes e nomes

| MCP | SRG | Papel |
|---|---|---|
| `net.minecraft.block.BlockLiquid` | – | base abstrata: altura, vetor de fluxo, cor, lava × água |
| `BlockLiquid#getLiquidHeightPercent(int)` | `func_149801_b` | **fração de ar** de um nível: `(decay + 1) / 9`, com `decay >= 8` tratado como `0` |
| `BlockLiquid#func_149804_e(World,III)` | `func_149804_e` | decay bruto da célula, ou `-1` se o material é outro |
| `BlockLiquid#getEffectiveFlowDecay(IBlockAccess,III)` | `func_149798_e` | decay com queda (`>= 8`) convertida em `0`; `-1` se outro material |
| `BlockLiquid#getFlowVector(IBlockAccess,III)` | `func_149800_f` | vetor de fluxo normalizado (**`private`**) |
| `BlockLiquid#velocityToAddToEntity(World,III,Entity,Vec3)` | `func_149640_a` | soma `getFlowVector` ao acumulador |
| `BlockLiquid#getFlowDirection(IBlockAccess,III,Material)` | `func_149802_a` | ângulo do fluxo para a textura (cliente, `static`) |
| `BlockLiquid#colorMultiplier(IBlockAccess,III)` | `func_149720_d` | média 3×3 de `BiomeGenBase#getWaterColorMultiplier` |
| `BlockLiquid#shouldSideBeRendered` / `isBlockSolid` | `func_149646_a` / `func_149747_d` | culling de faces e teste de parede |
| `BlockLiquid#tickRate(World)` | `func_149738_a` | água 5; lava 30 (10 em mundo sem céu) |
| `net.minecraft.block.BlockDynamicLiquid` | – | `flowing_water` (id 8), `flowing_lava` (id 10): o bloco que **simula** |
| `BlockDynamicLiquid#updateTick` | `func_149674_a` | o algoritmo de fluxo |
| `BlockDynamicLiquid#func_149811_n` | idem | vira o bloco estático (`id + 1`), `setBlock(..., flag 2)` |
| `BlockDynamicLiquid#func_149813_h` | idem | escreve o fluido numa célula vizinha (derruba o bloco que estava lá) |
| `BlockDynamicLiquid#func_149812_c` / `func_149808_o` | idem | busca de declive (recursão até 4) e direções ótimas |
| `BlockDynamicLiquid#func_149807_p` | idem | "esta célula bloqueia o fluxo?" |
| `BlockDynamicLiquid#func_149809_q` | idem | "o fluido pode ocupar esta célula?" |
| `net.minecraft.block.BlockStaticLiquid` | – | `water` (id 9), `lava` (id 11): parado; acorda em `onNeighborBlockChange` |
| `BlockStaticLiquid#setNotStationary` | `func_149818_n` | volta para `id - 1` e agenda o tick |
| `World#handleMaterialAcceleration(AxisAlignedBB,Material,Entity)` | `func_72918_a` | "está na água?" + empurrão |
| `World#isAnyLiquid` / `isMaterialInBB` / `isAABBInMaterial` | `func_72953_d` / `func_72875_a` / `func_72830_b` | consultas de fluido por caixa |
| `World#checkBlockCollision` | `func_72829_c` | qualquer bloco não-ar na caixa |
| `Entity#handleWaterMovement()` | `func_70072_I` | atualiza `inWater` (`field_70171_ac`), splash, apaga fogo |
| `Entity#isInWater()` / `isWet()` | `func_70090_H` / `func_70026_G` | leitura do estado |
| `Entity#isInsideOfMaterial(Material)` | `func_70055_a` | olhos dentro do material |
| `Entity#handleLavaMovement()` | `func_70058_J` | lava por `isMaterialInBB` |
| `Entity#isOffsetPositionInLiquid(DDD)` | `func_70038_c` | usado para sair da água numa borda |
| `Entity#isPushedByWater()` | `func_96092_aw` | jogador voando devolve `false` |
| `EntityLivingBase#moveEntityWithHeading(FF)` | `func_70612_e` | arraste, gravidade e pulo de borda na água |
| `ActiveRenderInfo#getBlockAtEntityViewpoint` | `func_151460_a` | bloco "onde a câmera está" (névoa, FOV) |
| `EntityRenderer#updateFogColor(F)` / `setupFog(IF)` | `func_78466_h` / `func_78468_a` | cor e densidade da névoa |
| `RenderBlocks#renderBlockLiquid` / `getLiquidHeight` | `func_147721_p` / `func_147729_a` | malha do líquido e altura por canto |
| `RenderBlocks#renderBlockByRenderType` | `func_147805_b` | despacho por `getRenderType()` (líquido = 4) |
| `WorldRenderer#updateRenderer(EntityLivingBase)` | `func_147892_a` | laço de meshing vanilla (passes 0 e 1) |
| `BiomeGenBase#getWaterColorMultiplier()` | sem SRG (método acrescentado pelo Forge) | dispara `BiomeEvent.GetWaterColor` |
| `BiomeGenBase#waterColorMultiplier` | `field_76759_H` | `0xFFFFFF` por padrão; pântano `14745518` (`0xE0FFAE`) |
| `Chunk#func_150807_a(IIILBlock;I)` | idem | o `setBlock` do chunk |
| `Chunk#func_150812_a(IIILTileEntity;)` | idem | anexa a TileEntity à posição |
| `ExtendedBlockStorage#getExtBlockMetadata` / `setExtBlockMetadata` | `func_76665_b` / `func_76654_b` | metadata da seção |
| `WorldServer#tickUpdates(Z)` | `func_72955_a` | entrega dos ticks agendados |
| `ItemBucket#tryPlaceContainedLiquid` | `func_77875_a` | despejo do balde |

## 2. Modelo de dados

* Um bloco por célula: id em `blockLSBArray` (`field_76680_d`) + `blockMSBArray` (`field_76681_e`), metadata de 4 bits em
  `blockMetadataArray` (`field_76678_f`). Não existe estado de fluido separado: a água **é** o bloco.
* Metadata do líquido: `0` = fonte; `1..7` = corrente (decay crescente = nível mais baixo); `>= 8` = em queda (o valor
  guarda `decay + 8` do bloco de cima, mas todo o código trata `>= 8` como `0`).
* Dois blocos por fluido. O dinâmico (`BlockDynamicLiquid`) simula num tick agendado; quando o nível calculado é igual ao
  atual ele se troca pelo estático (`func_149811_n`, `BlockDynamicLiquid.java:20-24`). O estático volta a dinâmico em
  `onNeighborBlockChange` (`BlockStaticLiquid.setNotStationary`). **Consequência para o waterlogging**: o tick agendado é
  entregue só se o bloco na posição ainda é o agendado (`WorldServer.tickUpdates`: `Block.isEqualTo(block,
  entry.func_151351_a())`, `WorldServer.java:529` em diante). Um tick de `flowing_water` agendado numa laje é descartado.
* `ItemBucket` segura `Blocks.flowing_water` (não `Blocks.water`); `tryPlaceContainedLiquid` recusa a célula se ela não é ar
  e o material é sólido, e destrói blocos não sólidos antes de escrever o fluido.

## 3. Altura do fluido: cinco fórmulas diferentes

`getLiquidHeightPercent(decay)` devolve a fração de **ar**, `(decay + 1) / 9`. A superfície desenhada de uma fonte fica
em `y + 8/9`. O jogo não usa uma única definição de altura:

| Onde | Fórmula da superfície | Fonte (decay 0) | Decay 7 |
|---|---|---|---|
| Render: `RenderBlocks.getLiquidHeight` (`:4343`) | `1 - (decay+1)/9`; `1` se há o mesmo material em cima | `8/9` | `1/9` |
| Câmera: `ActiveRenderInfo.getBlockAtEntityViewpoint` (`:85`) | `1 - ((decay+1)/9 - 0.11111111)` = `1 - decay/9` | `1` | `2/9` |
| Olhos: `Entity.isInsideOfMaterial` (`Entity.java:1136`, patch do Forge) | `filled = 1` para todo bloco que não é `IFluidBlock` | `1` | `1` |
| Corpo: `World.handleMaterialAcceleration` (`World.java:2526`) | compara `(y+1) - (decay+1)/9` com `floor(maxY + 1)`, um **inteiro** | sempre dentro | sempre dentro |
| Anzol: `World.isAABBInMaterial` (`World.java:2611`) | `(y+1) - decay/8` (`y+1` se `decay >= 8`) | `1` | `1/8` |

Pontos que importam:

* `handleMaterialAcceleration` faz `if ((double) l >= d0)` com `l = floor(box.maxY + 1.0D)`. Como a célula visitada tem
  `l1 < l`, vale `d0 < l1 + 1 <= l` e o teste é sempre verdadeiro. Toda célula de água cujo cubo cruza a caixa conta,
  qualquer que seja o nível.
* O Forge trocou o cálculo de `isInsideOfMaterial`: para `BlockLiquid` (que não implementa `IFluidBlock`) o resultado é
  `d0 < j + 1`, ou seja, olho em qualquer altura da célula.
* A diferença `8/9` (desenho) contra `1` (`isAABBInMaterial`) é a que o Kunftig documenta em `docs/fishing/FISICA.md` e
  contorna no `ModernBobber`.

## 4. Vetor de fluxo (`getFlowVector`, `BlockLiquid.java:206-311`)

```
l = effectiveDecay(self)
para cada vizinho horizontal n (oeste, norte, leste, sul):
    l1 = effectiveDecay(n)                      # -1 se o material é outro
    se l1 < 0:
        se !material(n).blocksMovement():
            l1 = effectiveDecay(n abaixo)
            se l1 >= 0:  v += dir(n) * (l1 - (l - 8))
    senão:               v += dir(n) * (l1 - l)
se metadata(self) >= 8 (em queda):
    se isBlockSolid em qualquer uma de 8 posições (4 lados em y e 4 em y+1):
        v = normalize(v) + (0, -6, 0)
return normalize(v)
```

* `isBlockSolid` do líquido (`:156`): mesmo material → `false`; lado 1 → `true`; gelo → `false`; senão
  `Block.isBlockSolid`, que é `material.isSolid()`.
* Um vizinho de **outro** líquido (lava ao lado de água) cai no ramo `l1 < 0`: lava não bloqueia movimento, então o
  código olha a célula abaixo dela.
* O método é `private`. O Angelica já o expõe com um `@Invoker` (`BlockLiquidFlowInvoker`).

## 5. Entidades na água

### 5.1 Detecção (`Entity.handleWaterMovement`, `Entity.java:1082-1126`)

```java
this.worldObj.handleMaterialAcceleration(
    this.boundingBox.expand(0.0D, -0.4000000059604645D, 0.0D).contract(0.001D, 0.001D, 0.001D),
    Material.water, this)
```

A caixa perde `0.4` em cima e embaixo antes da consulta. Para uma caixa de `0.6` de altura (pose de nado dos mods) a caixa
resultante é invertida (`minY + 0.401 > maxY - 0.401`): o laço `k..l` fica vazio em parte das frações de Y e `inWater`
oscila. O Kunftig registra esse efeito num comentário de `FlightClient`.

### 5.2 Empurrão (`World.handleMaterialAcceleration`)

Soma `getFlowVector` de todas as células de água da caixa. Se o comprimento é `> 0` e `entity.isPushedByWater()`:
normaliza e soma `0.014` a `motionX/Y/Z`. Não há média por célula, piso de velocidade nem escala por profundidade.

### 5.3 Movimento (`EntityLivingBase.moveEntityWithHeading`, `EntityLivingBase.java:1578-1596`)

```java
this.moveFlying(strafe, forward, this.isAIEnabled() ? 0.04F : 0.02F);
this.moveEntity(this.motionX, this.motionY, this.motionZ);
this.motionX *= 0.800000011920929D;
this.motionY *= 0.800000011920929D;
this.motionZ *= 0.800000011920929D;
this.motionY -= 0.02D;
if (this.isCollidedHorizontally && this.isOffsetPositionInLiquid(motionX, motionY + 0.6000000238418579D - posY + d0, motionZ))
    this.motionY = 0.30000001192092896D;
```

* Ordem: aceleração, movimento, arraste `0.8` nos três eixos, gravidade `0.02`.
* Velocidade terminal de afundamento: ponto fixo de `v = 0.8·v − 0.02` → **−0.1 bloco/tick**.
* Pulo na água (`onLivingUpdate`, `EntityLivingBase.java:1998-2010`): `motionY += 0.03999999910593033D` por tick enquanto
  `isJumping`; subida terminal `u = 0.8·u − 0.02 + 0.04` → **+0.1 bloco/tick**.
* Aceleração horizontal: `0.02` (jogador e entidades sem IA nova) ou `0.04` (`isAIEnabled()`).
* Lava: mesma forma com arraste `0.5`.
* Os três `0.800000011920929D` seguidos de `DMUL` são a impressão digital do Akashic Player Backport (ver
  [COMPATIBILIDADE.md](COMPATIBILIDADE.md#akashic-player-backport)).

## 6. Simulação de fluxo (`BlockDynamicLiquid.updateTick`, `BlockDynamicLiquid.java:29-170`)

1. Se não é fonte (`l > 0`): menor decay entre os quatro vizinhos (`func_149810_a`) + queda de nível (`1`; lava no
   mundo normal `2`). `>= 8` ou sem vizinho → some (`-1`). Com o mesmo fluido em cima: vira queda (`k1 >= 8 ? k1 : k1 + 8`).
2. Água com **2 ou mais fontes vizinhas** (decay exatamente `0`; quedas não contam) vira fonte se embaixo há material
   sólido ou outra fonte.
3. Nível igual ao atual → vira estático. Diferente → `setBlockMetadataWithNotify(flag 2)` + novo tick +
   `notifyBlocksOfNeighborChange`; `< 0` → `setBlockToAir`.
4. Se pode descer (`func_149809_q` abaixo): escreve queda embaixo. Senão, se é fonte ou o de baixo bloqueia: espalha
   para as direções de menor distância a um buraco (`func_149808_o`, recursão até 4) com `decay + queda` (`1` se vem
   de queda); para em `>= 8`.
5. `func_149807_p` (bloqueia): portas de madeira e ferro, placa de pé, escada de mão, cana → sim; material portal →
   sim; senão `material.blocksMovement()`.
6. `func_149813_h`: se a célula aceita, derruba o bloco que estava lá (`dropBlockAsItem`) e faz
   `setBlock(x, y, z, this, nível, 3)`.

## 7. Render

* `getRenderType() = 4`; `getRenderBlockPass()` = 1 para água, 0 para lava.
* `renderBlockLiquid` (`RenderBlocks.java:4138`): cor por `block.colorMultiplier`; faces por
  `block.shouldSideBeRendered`; quatro alturas de canto por `getLiquidHeight`; recuo de `0.001` no topo e nos lados;
  sombreamento fixo `0.5 / 1.0 / 0.8 / 0.6`; textura parada (`getIcon(1, …)`) ou corrente (`getIcon(2, …)`) conforme
  `getFlowDirection`.
* `getLiquidHeight`: para as 4 células do canto: mesmo material em cima → `1`; mesmo material → soma a fração de ar
  (fonte e queda com peso 10); não sólido → soma `1` (ar); sólido → ignora.
* `WorldRenderer.updateRenderer` (`:128`): o passe 1 só roda se algum bloco do passe 0 devolveu
  `getRenderBlockPass() > 0` (`:197`), e cada bloco é desenhado só se `canRenderInPass(pass)` (`:204`).
* Texturas (`assets/minecraft/textures/blocks/`), medidas no jar do cliente:

  | Arquivo | Tamanho | Quadros | Cor média | Alfa |
  |---|---|---|---|---|
  | `water_still.png` | 16×512 | 32 (`frametime 2`) | `#2F43F4` (já azul) | 168 a 226, média 179 |
  | `water_flow.png` | 32×1024 | 32 | `#3148F4` | 165 a 211, média 175 |
  | `misc/underwater.png` | 16×16 | 1 | `#2A5EFF` | média 138 |

* Cor: `BlockLiquid.colorMultiplier` faz a média 3×3 de `getWaterColorMultiplier()`, que no Forge é:

  ```java
  BiomeEvent.GetWaterColor event = new BiomeEvent.GetWaterColor(this, waterColorMultiplier);
  MinecraftForge.EVENT_BUS.post(event);
  return event.newColor;
  ```

  A textura já é azul; o multiplicador padrão é branco. Só o pântano muda a cor no vanilla.

## 8. Névoa e câmera

* `ActiveRenderInfo.getBlockAtEntityViewpoint` devolve o bloco da célula da câmera (ou o de cima, se a câmera está acima
  da superfície pela fórmula da seção 3).
* `EntityRenderer.updateFogColor` (`:1754`): material água → `(0.02 + r, 0.02 + r, 0.2 + r)` com
  `r = respiration × 0.2`. No fim dispara `EntityViewRenderEvent.FogColors` (`:1918`).
* `EntityRenderer.setupFog` (`:1932`): dispara `EntityViewRenderEvent.FogDensity` **antes** de tudo (`:1964`); se
  cancelado, só `glFogf(GL_FOG_DENSITY, event.density)`. Senão, água → `GL_EXP`, densidade `0.1`
  (`0.05` com respiração aquática; `0.1 − respiration × 0.03`). `RenderFogEvent` só é disparado no ramo padrão (fora de
  água e lava).
* `EntityRenderer.getFOVModifier` (`:496`): material água → `fov × 60 / 70`.
* O contorno de seleção muda com `isInsideOfMaterial(Material.water)` (`:1309`, `:1325`).

## 9. TileEntities

`Chunk.func_150812_a` (`Chunk.java:953`) só guarda a TileEntity se `getBlock(x, y, z).hasTileEntity(metadata)`, e
**invalida a que já estava** na mesma posição. Há no máximo uma TileEntity por célula.

## 10. A verificar no decompilador

* **[A VERIFICAR]** `EntityItem.onUpdate`, `EntityBoat.onUpdate`, `EntityXPOrb`, `EntityFishHook.onUpdate` e
  `EntityArrow`: cada um tem física própria na água e não foi lido nesta fase.
* **[A VERIFICAR]** `ItemRenderer.renderOverlays` / `renderWaterOverlayTexture` (a sobreposição `underwater.png`).
* **[A VERIFICAR]** `EntityPlayer.onLivingUpdate` e `EntityPlayerSP` em relação a nado, bolhas de ar e
  `canBreatheUnderwater`.
* **[A VERIFICAR]** o bytecode real dessas classes no `server.jar` do Crucible (ver
  [COMPATIBILIDADE.md](COMPATIBILIDADE.md#crucible)).
