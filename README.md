# Neo States

Backport da dinâmica de líquidos do Minecraft 1.21.1 para o 1.7.10 (Forge 10.13.4.1614), para o modpack TEOF:
servidor Crucible em Java 21, clientes em Java 8. Começa pela água, em três camadas:

| Camada | Conteúdo |
|---|---|
| A, visual | textura, cor e névoa submersa por bioma |
| B, física | leitura unificada de altura, movimento do jogador na água |
| C, estado | waterlogging: blocos parciais com água na mesma célula |

**Estado: Fase 9.** Há a API de leitura de fluido, a Camada A (textura em cinza, cor por bioma, névoa submersa) e os
Camada C até o comportamento: lajes, escadas, cercas, muros, painéis, alçapões, escadas de mão, placas e baús podem
conter água (inclusive em Y < 0), com balde, colocar e quebrar; a água conta para entidades, espalha e é desenhada
(testado com o Angelica; sem ele falta ver dentro de um mundo). O fluxo dela ainda não avisa plugins de proteção. Testado no pack real: [docs/TESTES.md](docs/TESTES.md).

```bash
VERSION=0.1.0-alpha.1 ./gradlew spotlessApply build -x javadoc
```

Se o Maven Central responder 429, repita com `--max-workers=1`.

Comece por [docs/FASE-0.md](docs/FASE-0.md).

| Documento | Conteúdo |
|---|---|
| [docs/FASE-0.md](docs/FASE-0.md) | síntese, decisões pendentes, lista do que falta verificar |
| [docs/VANILLA-1.7.10.md](docs/VANILLA-1.7.10.md) | a água no 1.7.10 |
| [docs/MODERN-1.21.1.md](docs/MODERN-1.21.1.md) | a água no 1.21.1 |
| [docs/DELTAS.md](docs/DELTAS.md) | o que mudou e o que vale portar |
| [docs/COMPATIBILIDADE.md](docs/COMPATIBILIDADE.md) | convivência com BelowCore, Player Backport, Kunftig, Angelica, Et Futurum, Crucible e os demais |
| [docs/adr/](docs/adr/) | decisões de arquitetura |

## Configuração

`config/neostates.cfg`, uma opção por parte. As de `physics` e `state` que os dois lados precisam ter iguais
(`modernWaterPhysics`, `modernWaterLight`, `floatingItems`, `modernAirRecovery`, `waterlogging`) valem pelo servidor: ele as manda ao cliente na entrada.
Num servidor sem o Neo States o cliente as desliga sozinho.

| Opção | Padrão | Desliga |
|---|---|---|
| `visual.modernWaterColor` | `true` | textura em cinza e cor por bioma |
| `visual.modernWaterFog` | `true` | névoa submersa do 1.21.1 |
| `physics.modernWaterPhysics` | `true` | "na água" pela superfície real, empurrão, afundamento, pulo, descida agachado |
| `physics.modernWaterLight` | `true` | a água tirar só 1 de luz por bloco |
| `physics.floatingItems` | `true` | itens e orbes de experiência boiarem e seguirem a correnteza (os itens precisam de `modernWaterPhysics`) |
| `physics.modernAirRecovery` | `true` | o ar voltar aos poucos fora d'água, com as bolhas na tela enquanto volta |
| `physics.relightOldChunks` | `true` | refazer, uma vez por chunk, a luz da água gerada antes da mudança |
| `physics.relightMillisPerTick` | `2` | (tempo máximo por tick gasto nisso) |
| `state.waterlogging` | `true` | blocos poderem conter água (a água já guardada fica no mundo, sem efeito) |

Com `modernWaterPhysics` ligado, o empurrão de líquidos do Et Futurum (`adjustedLiquidPhysics`) deixa de valer para a
água; o da lava continua. Com `floatingItems` ligado, a flutuação de itens do Et Futurum (`liquidItemFloat`) também
deixa de valer na água, e continua na lava.

O que não foi verificado em código ou em jogo aparece nos documentos como **[A VERIFICAR]**.
