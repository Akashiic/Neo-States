# Histórico

Versões de desenvolvimento, uma por fase. Nenhuma foi publicada como release; o registro dos testes de cada uma está
em [docs/TESTES.md](docs/TESTES.md).

## 0.1.0-alpha.10 (Fase 10)

* Flecha na água guarda 0,6 do movimento por tick, como no 1.21.1 (era 0,8).
* Build automático no GitHub (`.github/workflows/build.yml`), que publica o jar do mod e o da API.
* Verificado em jogo: mordida e fisgada com o anzol do Kunftig em água dentro de lajes; item levado pela correnteza;
  trilho com água.

## 0.1.0-alpha.9 (Fase 9)

* Itens e orbes de experiência boiam e seguem a correnteza (`physics.floatingItems`). Com isso ligado, a flutuação de
  itens do Et Futurum deixa de valer na água.
* Fora d'água o ar volta 4 por tick, com as bolhas na tela enquanto volta (`physics.modernAirRecovery`).
* Trilhos contêm água; a água corrente não entra mais na célula de um bloco que pode contê-la.

## 0.1.0-alpha.8 (Fase 8)

* A água sai de um bloco que a contém só pelas faces abertas dele (laje, escada, alçapão, escada de mão).
* Correção: o pulo no raso não acontecia para o jogador local.

## 0.1.0-alpha.7 (Fase 7)

* A luz da água dos chunks gerados antes da mudança é refeita uma vez, ao carregar (`physics.relightOldChunks`,
  `physics.relightMillisPerTick`).
* O servidor manda as opções ao cliente, que as segue enquanto conectado.
* Um bloco que contém água, entre duas fontes e sobre bloco sólido ou fonte, enche.
* O fluxo da água hospedada é reagendado quando o chunk carrega.
* API 4 (`isSourceAt`), documentada em [docs/API.md](docs/API.md).

## 0.1.0-alpha.6 (Fase 6)

* Física da água do 1.21.1 (`physics.modernWaterPhysics`): "na água" pela superfície real, empurrão da correnteza,
  afundar a 0,005 por tick, pulo no raso, descer agachado.
* A água tira 1 de luz por bloco (`physics.modernWaterLight`).
* A água hospedada dispara o `BlockFromToEvent` do Bukkit antes de escorrer.
* `state.waterlogging` desliga a camada inteira.

## 0.1.0-alpha.5 (Fase 5)

* A água dentro de blocos é desenhada, com e sem Angelica.

## 0.1.0-alpha.4 (Fase 4)

* Blocos que contêm água: colocar, quebrar, balde, consultas de fluido, espalhamento. API 3. `/neostates autoteste`.

## 0.1.0-alpha.3 (Fase 3)

* A camada de waterlogging: 1 bit por célula, salva no chunk e sincronizada por pacotes próprios; vale abaixo de Y 0
  com o BelowCore. API 2.

## 0.1.0-alpha.2 (Fase 2)

* Textura da água em cinza, cor por bioma e névoa submersa do 1.21.1 (`visual.modernWaterColor`,
  `visual.modernWaterFog`).

## 0.1.0-alpha.1 (Fase 1)

* Esqueleto do mod, API de leitura, `/neostates sonda`.
