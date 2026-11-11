# Neo States

Backport da dinâmica de líquidos do Minecraft 1.21.1 para o 1.7.10 (Forge 10.13.4.1614), para o modpack TEOF:
servidor Crucible em Java 21, clientes em Java 8. Começa pela água, em três camadas:

| Camada | Conteúdo |
|---|---|
| A, visual | textura, cor e névoa submersa por bioma |
| B, física | leitura unificada de altura, movimento do jogador na água |
| C, estado | waterlogging: blocos parciais com água na mesma célula |

**Estado: Fase 1.** Há o esqueleto do mod, a API de leitura de fluido e os testes de impressão digital. Nenhum
comportamento do jogo muda ainda. Não foi carregado em jogo nem em servidor.

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

O que não foi verificado em código ou em jogo aparece nos documentos como **[A VERIFICAR]**.
