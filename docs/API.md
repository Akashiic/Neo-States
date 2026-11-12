# API do Neo States

Classe `com.akashic.neostates.api.NeoStates`, versão **4** (`API_VERSION` / `apiVersion()`). É pequena e estável de
propósito: dá para chamar por reflexão, sem dependência de compilação, e é assim que o Akashic Player Backport e o
Kunftig a usam (`compat/NeoStatesBridge` em cada um). Quem preferir compilar contra ela usa o
`NeoStates-<versão>-api.jar` como `compileOnly`; ele só tem o pacote `api`.

Tudo é chamado na thread do mundo (servidor) ou na thread do cliente. Nada aqui carrega chunk: uma célula fora do que
está carregado responde "sem fluido".

| Método | Desde | O que responde |
|---|---|---|
| `int apiVersion()` | 1 | a versão desta tabela |
| `int fluidAt(IBlockAccess, x, y, z)` | 1 | o fluido da célula num `int` (ver `FluidView`): tipo, nível, fonte, caindo, hospedado |
| `boolean isWaterAt(IBlockAccess, x, y, z)` | 1 | há água na célula, em bloco de água **ou** dentro de um bloco |
| `double surfaceY(IBlockAccess, x, y, z)` | 1 | a altura absoluta da superfície, como entidades e anzóis a veem; `NaN` sem fluido. Fonte: `y + 8/9`; célula sob mais fluido: `y + 1` |
| `boolean isWaterlogged(World, x, y, z)` | 2 | o bloco da célula contém água |
| `boolean setWaterlogged(World, x, y, z, boolean)` | 2 | põe ou tira a água de um bloco que pode contê-la; só no servidor; devolve se mudou. Sincroniza os clientes e agenda o fluxo |
| `boolean canHoldWater(Block)` | 3 | o bloco é dos que podem conter água (e `state.waterlogging` está ligado) |
| `boolean isSourceAt(World, x, y, z)` | 4 | a célula é uma fonte: bloco de água no nível 0 ou bloco com água |

## Como chamar sem depender do jar

```java
Class<?> api = Class.forName("com.akashic.neostates.api.NeoStates");
MethodHandle isWaterAt = MethodHandles.publicLookup().findStatic(api, "isWaterAt",
    MethodType.methodType(boolean.class, IBlockAccess.class, int.class, int.class, int.class));
```

`ClassNotFoundException` quer dizer "Neo States não instalado": siga com a checagem vanilla. Confira `apiVersion()`
antes de usar um método mais novo que o 1.

## Quem usa

| Mod | Para quê | Estado |
|---|---|---|
| Akashic Player Backport | as condições de nadar e rastejar também valem em água dentro de blocos (`isWaterAt`, `surfaceY`) | os dois sobem juntos no TEOF 5.4 e o contrato dos três `0.8` continua; nadar dentro de lajes com jogador real: **[A VERIFICAR]** |
| Kunftig | o anzol pergunta a superfície (`surfaceY`) antes da checagem vanilla | verificado em jogo: a boia fica na superfície sobre lajes com água ([imagem](evidencias/fase-7/pesca-boia-na-superficie.jpg)), e isca, mordida e fisgada acontecem nessa água (Fase 10) |

## O que a API não faz

* não expõe a fila de fluxo nem a camada de bits: quem precisa de água num bloco usa `setWaterlogged`;
* não há evento próprio. No Crucible, o fluxo da água hospedada dispara o `BlockFromToEvent` do Bukkit, como a água
  vanilla.
