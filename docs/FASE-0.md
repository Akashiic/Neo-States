# Fase 0: levantamento técnico

Síntese do levantamento. Os detalhes estão nos documentos ligados; aqui ficam as conclusões, o que mudou em relação ao
enunciado original, as decisões que dependem do dono do projeto e a lista do que falta verificar.

Nenhum código de mod nem mixin foi escrito nesta fase.

| Documento | Conteúdo |
|---|---|
| [VANILLA-1.7.10.md](VANILLA-1.7.10.md) | como a água funciona no 1.7.10 (classes, nomes MCP e SRG, fórmulas) |
| [MODERN-1.21.1.md](MODERN-1.21.1.md) | como funciona no 1.21.1 (Mojang Mappings) |
| [DELTAS.md](DELTAS.md) | comparação item a item, com a decisão de portar ou não |
| [COMPATIBILIDADE.md](COMPATIBILIDADE.md) | o que cada mod do ecossistema já faz na água, e as regras que saem disso |
| [adr/ADR-0001](adr/ADR-0001-armazenamento-waterlogging.md) | onde guardar o waterlogging |
| [adr/ADR-0002](adr/ADR-0002-interceptacao-visual.md) | onde interceptar cor, névoa e desenho, com e sem Angelica |
| [adr/ADR-0003](adr/ADR-0003-pipeline-de-fluidos.md) | API e pipeline de altura e fluxo |

## O pedido ficou claro?

O objetivo e o formato ficaram claros. Seis pontos do enunciado mudaram depois de ler o código, e cinco decisões
dependem de você (seção [Decisões do dono](#decisões-do-dono)).

## O que mudou em relação ao enunciado

1. **"Limite de 4 bits" não é mais o problema.** No TEOF o EndlessIDs já estende o metadata armazenado para 16 bits.
   O limite real é de semântica: cada bloco é dono do próprio metadata. E no 1.21.1 o waterlogging é só **1 bit por
   bloco** (o fluido é sempre água fonte; o `FluidState` é derivado do `BlockState`, não armazenado).
2. **Já existe um mod de waterlogging para 1.7.10**: o FluidLogged (MEGA / FalsePattern), e o Angelica tem integração
   de dados com ele. Ele valida a arquitetura, mas não serve como está: não funciona em Y < 0, o Angelica não desenha
   o fluido dele, e ele colide com o Et Futurum e o Hodgepodge em dois pontos.
3. **A física de fluxo não mudou.** `BlockLiquid.getFlowVector` e `FlowingFluid.getFlow` são a mesma função (diferem
   por um fator constante antes da normalização). A tabela de alturas também é a mesma. Não há motor `FlowableFluid`
   a portar.
4. **O que mudou de verdade é a leitura.** O 1.7.10 tem cinco fórmulas de altura diferentes (render, câmera, olhos,
   corpo, anzol); o 1.21.1 tem uma. E mudou o movimento do jogador na água: gravidade 4× menor.
5. **Boa parte da Camada B já tem dono.** O Et Futurum Requiem já altera o empurrão de líquidos, faz itens flutuarem e
   tem coluna de bolhas, tudo ligado por padrão. O Akashic Player Backport já portou o arraste de nado e o nado guiado
   pelo olhar.
6. **Cor e névoa não precisam de mixin no pipeline gráfico.** O Forge já dispara `BiomeEvent.GetWaterColor`,
   `EntityViewRenderEvent.FogColors` e `FogDensity`, e o Angelica mantém os três. Os métodos de névoa do
   `EntityRenderer` já têm injeções do BelowCore e do Angelica que não toleram vizinhos.

## Base de evidências

| Fonte | Como foi obtida |
|---|---|
| Minecraft 1.7.10 + Forge 10.13.4.1614, MCP `stable_12` | `./gradlew decompressDecompiledSources` (RetroFuturaGradle) num dos mods; fontes em `build/rfg/minecraft-src/java`. `client.jar` sha1 `e80d9b3bf5085002218d4be59e668bac718abbc6` |
| Minecraft 1.21.1, Mojang Mappings | `client.jar` (sha1 `30c73b1c5da787909b2f73340419fdf13b9def88`) e `client.txt` de `piston-data.mojang.com`; remapeado com ForgeAutoRenamingTool 1.1.2 (`--reverse`); decompilado com Vineflower 1.11.1 |
| Texturas e cores de bioma | medidas nos dois `client.jar` (PNG e `data/minecraft/worldgen/biome/*.json`) |
| Angelica 2.2.43 (e 2.2.37, 2.2.45 para comparar), Hodgepodge 2.7.189, GTNHLib 0.11.40, Et Futurum Requiem 2.6.59-GTNH, ChunkAPI 0.8.4, EndlessIDs 1.7.4 | jars `-sources` de `nexus.gtnewhorizons.com` |
| FluidLogged 0.2.0 | jar `-sources` de `mvn.falsepattern.com/gtmega_releases` |
| Crucible | `git clone` de `CrucibleMC/Crucible`, commit `cd1de3d`; lidos os arquivos de `patches/` |
| BelowCore, Akashic Player Backport, Kunftig, YesSteveModel, VitaGenesis, AkashicFishing | os repositórios desta sessão |

Parte do inventário dos mods foi feita por dois levantamentos paralelos; os pontos que sustentam decisões (os mixins do
BelowCore, a impressão digital do Player Backport, o anzol do Kunftig, a física de líquidos do Et Futurum, o
`updateTick` do Hodgepodge, o `DeepDataManager`) foram conferidos de novo no código.

O que **não** foi feito: nada foi executado em jogo nem em servidor. Tudo o que depende de execução está marcado
**[A VERIFICAR]**.

## 1. Deltas físicos (resumo)

Detalhe em [DELTAS.md](DELTAS.md).

| Tema | 1.7.10 | 1.21.1 | Veredito |
|---|---|---|---|
| Altura por nível | `1 − (decay+1)/9`; fonte `8/9` | `amount/9`; fonte `8/9` | **igual** |
| Vetor de fluxo | diferenças de decay, `normalize`, termo `−6` na queda | diferenças de altura, idem | **igual** (fator `1/9`) |
| Escala do empurrão | `0.014` | `0.014` | **igual** |
| Combinação do empurrão | soma + `normalize` | média; jogador sem `normalize`; × altura se `< 0.4`; piso `0.0045` | difere; o Et Futurum já mexe aqui |
| Caixa de "na água" | caixa cortada em `0.4` em cima e embaixo | caixa inteira (`deflate 0.001`) | **difere**; a caixa de 0.6 da pose de nado quebra no 1.7.10 |
| Critério de "na água" | qualquer célula de água cruzada (o teste de altura compara com um inteiro e é sempre verdadeiro) | superfície `>=` base da caixa | **difere** |
| Olhos na água | célula inteira (patch do Forge) | altura real | **difere** |
| Arraste na água | `0.8` nos três eixos | `0.8`; `0.9` horizontal correndo | já portado pelo Player Backport |
| Gravidade na água | `−0.02`/tick | `−0.005`/tick (`g/16`) | **difere** |
| Afundamento terminal | `−0.1` bloco/tick | `−0.025` bloco/tick | **difere** (4×) |
| Subida terminal segurando pulo | `+0.1` | `+0.175` | **difere** |
| Descer agachado | não existe | `−0.04`/tick (`−0.225` terminal) | novo |
| Intervalo de tick, queda de nível, busca de declive | 5, 1, 4 | 5, 1, 4 | **igual** |
| Fonte infinita | 2 fontes + sólido ou fonte embaixo | idem + gamerule | **igual** por padrão |
| Espalhar para os lados ao descer | nunca | com 3+ fontes vizinhas | difere |
| Arquitetura | 2 blocos por fluido, ticks de bloco | 1 bloco, `FluidState` derivado, fila de ticks de fluido | não portar; a Camada C precisa de ticks próprios |

## 2. Waterlogging (resumo do ADR-0001)

**Veredito: camada paralela de 1 bit por célula, presa a cada `ExtendedBlockStorage` por um campo `@Unique`, salva e
sincronizada pelo ChunkAPI.** É a Abordagem A com âncora na seção.

| Critério | A (na seção, via ChunkAPI) | B (TileEntity fantasma) | C (arrays da seção) |
|---|---|---|---|
| RAM | 512 bytes por seção com algum bloco waterlogged; zero nas outras | um objeto + entrada de mapa + NBT por bloco | 2 KB a 4 KB por seção, sempre |
| Leitura em laço de física | campo em `Block` → array nulo? → 1 bit; sem segunda busca de chunk | `HashMap` com alocação por consulta | igual ao metadata |
| Angelica | a cópia da seção já leva o dado (`cloneSubChunk`) | mapa concorrente com trava por seção | o Angelica copia só os arrays que conhece |
| Y < 0 (BelowCore) | automático: as seções profundas usam os managers de seção | precisa de código | conflita com os `@Redirect` do BelowCore |
| EndlessIDs | campo novo, sem cruzar | indiferente | conflito direto de `@Overwrite` |
| TileEntity existente | intacta | **destruída** (`Chunk.func_150812_a` invalida a anterior) | intacta |
| Bloco sem TileEntity | funciona | **recusado** (`hasTileEntity(meta)` é falso) | funciona |

A variante "usar os bits altos do metadata do EndlessIDs" também foi avaliada e rejeitada: os blocos reescrevem o
próprio metadata e indexam arrays de ícone por ele.

A água hospedada espalha por rotina própria, que só escreve nos vizinhos. **A célula hospedeira nunca recebe
`setBlock`**: é isso que mantém a TileEntity e o NBT de terceiros intactos.

## 3. Visual com o Angelica (resumo do ADR-0002)

O Angelica 2.2.43 não reimplementa o desenho do líquido: o laço de meshing chama `block.canRenderInPass(pass)` e
`RenderBlocks.renderBlockByRenderType`, que cai no `renderBlockLiquid` vanilla. Por isso a interceptação fica no
contrato vanilla:

| Efeito | Onde |
|---|---|
| Cor por bioma | evento `BiomeEvent.GetWaterColor` |
| Textura em cinza | sprite convertido em tempo de execução (os arquivos da Mojang não podem ir no jar) |
| Cor da névoa | evento `EntityViewRenderEvent.FogColors` |
| Distância da névoa | evento `EntityViewRenderEvent.FogDensity`, cancelado, com `glFog*` linear |
| Câmera e olhos em bloco waterlogged | `@WrapOperation` em `ActiveRenderInfo.getBlockAtEntityViewpoint` e `Entity.isInsideOfMaterial` (o uniforme `isEyeInWater` dos shaders usa este último) |
| Água de bloco waterlogged | `Block.canRenderInPass` + `renderBlockByRenderType`, com `Iris.setShaderMaterialOverride` para o shader |

Um único mixin toca numa classe do Angelica: um acesso ao array de seções do `WorldSlice`, opcional. Esse arquivo é
idêntico nas versões 2.2.37, 2.2.43 e 2.2.45.

## 4. Integração com o ecossistema (resumo do ADR-0003)

* **Nado (Player Backport + YesSteveModel)**. `isInWater()` e `isInsideOfMaterial()` passam pelas costuras, então o
  YesSteveModel funciona sem mudança. O Player Backport decide o **início** do nado lendo o material do bloco direto
  (`WaterPoseConditions`), o que nenhuma costura alcança: ele precisa chamar a API do Neo States. É uma mudança
  pequena no outro repositório.
* **Invariante do arraste**: `moveEntityWithHeading` tem de continuar com exatamente três `LDC 0.800000011920929D`
  seguidos de `DMUL`. A gravidade na água (delta D-3) mexe em outra constante (`0.02D`) e não toca nesses.
* **TileEntities**: ver o item 2.
* **Pesca**: a altura que o anzol do Kunftig usa (`8/9` na fonte) é a mesma do pipeline. Nada muda enquanto a altura
  lógica não mudar; a telemetria proposta compara as duas por anzol.
* **Ondas no futuro**: o pipeline separa altura lógica (nunca deforma), de interação (determinística e igual nos dois
  lados) e de render (livre).

## Regras de falha fechada

1. Toda costura com `require = 1`, sem `@Overwrite`, preferindo `@WrapOperation` / `@ModifyExpressionValue`.
2. Teste de impressão digital no build contra as classes reais do cliente e do `server.jar` do Crucible.
3. As oito regras de convivência de [COMPATIBILIDADE.md](COMPATIBILIDADE.md#regras-que-saem-deste-inventário).
4. Cada camada desligável por configuração, com o servidor mandando o valor para o cliente.
5. Recurso que encontra o alvo ocupado por outro mod se desliga e registra, em vez de somar efeitos (caso do empurrão
   com o `adjustedLiquidPhysics` do Et Futurum).
6. Telemetria de pesca, nado e empurrão antes de ligar qualquer delta de física.

## Decisões do dono

**Respondidas na Fase 1.** O dono aceitou as cinco recomendações, com três ressalvas que valem sobre a tabela:

| Tema | Decisão |
|---|---|
| Empurrão do fluido (linha 2) | mixin próprio, exatamente como no 1.21.1; não contar com o Et Futurum |
| Gravidade e pulo na água (linha 3) | exatamente como no 1.21.1, sem restringir ao jogador |
| Dependências (linha 5) | não depender do ChunkAPI nem de bibliotecas da FalsePattern ([ADR-0001, Revisão 1](adr/ADR-0001-armazenamento-waterlogging.md#revisão-1-sem-chunkapi)) |
| Novo | empurrão entre entidades e entre jogadores como no 1.21.1 ([DELTAS.md, seção I](DELTAS.md#i-empurrão-entre-entidades-a-levantar)) |
| Crucible | o trabalho no servidor entra quando chegar a hora; cliente e servidor carregam o mod |

| # | Decisão | Opções | Recomendação |
|---|---|---|---|
| 1 | Waterlogging: FluidLogged ou implementação própria | depender, bifurcar (LGPL), escrever | escrever, com o formato de 1 bit |
| 2 | Empurrão de líquidos | manter o do Et Futurum; desligar o dele e usar a fórmula do 1.21.1; não mexer | manter o do Et Futurum na primeira versão |
| 3 | Gravidade e pulo na água do 1.21.1 | só jogador; todas as entidades; não portar | só jogador, atrás de opção, depois da telemetria |
| 4 | Textura da água | converter em tempo de execução; resource pack do modpack | converter em tempo de execução |
| 5 | Licença do Neo States e dependência obrigatória do ChunkAPI | a definir | o ChunkAPI já é obrigatório no pack (EndlessIDs, BelowCore) |

Perguntas menores que travam detalhes: quais blocos de mods entram na lista padrão de "pode conter água"; se
estruturas geradas dentro d'água devem nascer waterlogged; se o `waterSourceConversion` vira gamerule.

## Estado

| Fase | Estado |
|---|---|
| 0 | concluída |
| 1 | esqueleto do mod, API de leitura, pipeline de altura, comando `/neostates sonda`, testes de impressão digital |
| 2 | Camada A: textura em cinza gerada em tempo de execução, cor por bioma, névoa submersa (opções `modernWaterColor` e `modernWaterFog` em `config/neostates.cfg`) |

O que foi verificado até a Fase 2:

* build e 28 testes;
* servidor de desenvolvimento (Forge, Java 21) sobe até `Done` com o mod;
* cliente de desenvolvimento (Java 8, sem tela, renderização por software) sobe até o menu: o mixin de `BlockLiquid`
  aplica e os dois sprites em cinza carregam (16×512 e 32×1024, 32 quadros cada).

O que **não** foi verificado: nada foi visto dentro de um mundo. A cor da água, a névoa e a textura animada em jogo,
com e sem Angelica e com shaders, continuam **[A VERIFICAR]**, assim como o Crucible e o pack real.

## Ordem proposta

| Fase | Entrega | Por quê nesta ordem |
|---|---|---|
| 1 | esqueleto do mod, testes de impressão digital (cliente + Crucible), `FluidView` e API de leitura só com blocos líquidos, telemetria | tudo depende da leitura unificada; ainda sem mudar comportamento |
| 2 | Camada A: cor, textura, névoa | só eventos; risco baixo; resultado visível |
| 3 | Camada C, dados: campo na seção, managers do ChunkAPI, comando de depuração, Y < 0 | fecha as verificações do ADR-0001 antes de qualquer costura |
| 4 | Camada C, leitura e escrita: costuras, balde, colocar e quebrar, ticks próprios | o comportamento |
| 5 | Camada C, render: vanilla e Angelica | depende da 3 e da 4 |
| 6 | Camada B: altura unificada para corpo, olhos e câmera; gravidade do jogador | atrás de opção, com telemetria |
| 7 | API usada pelo Player Backport e pelo Kunftig | mudanças nos outros repositórios |

## A verificar

Consolidado. Cada documento tem a própria lista com o contexto.

**No decompilador ou em `javap`**

* Bytecode final de `BlockDynamicLiquid`, `BlockLiquid`, `World`, `WorldServer`, `Chunk` e `Entity` no `server.jar`
  real do Crucible.
* Física própria de `EntityItem`, `EntityBoat`, `EntityXPOrb`, `EntityArrow` e `EntityFishHook` no 1.7.10, e dos
  equivalentes no 1.21.1.
* `ItemRenderer.renderOverlays` (1.7.10) e `ScreenEffectRenderer.renderWater` (1.21.1).
* `EntityPlayer.onLivingUpdate` com o PlayerAPI (`localOnLivingUpdate`).
* Constantes da coluna de bolhas do Et Futurum contra as do 1.21.1.
* `BlockPistonBase` do Et Futurum (`@Overwrite`): por onde o bloco movido é escrito.

**Em jogo ou servidor**

* Lista e versões reais do TEOF (os documentos citam números diferentes) e se `extendBlockItem` do EndlessIDs está
  ligado.
* Campo `@Unique` em `ExtendedBlockStorage` junto dos mixins do EndlessIDs, no cliente e no Crucible.
* Tamanho de pacote com o manager novo contra o tampão de 32 KB do BelowCore; `S23` em Y < 0.
* Velocity + ProtocolObelisk com o dado a mais nos pacotes de chunk.
* Ticks de fluido abaixo de Y 0.
* Angelica sem shader e com dois shaderpacks; vanilla puro.
* Custo medido de `handleMaterialAcceleration` com as costuras.
* `BlockFromToEvent` disparado pela rotina própria e respeitado por um plugin de proteção.
* Biomas de mods no `BiomeEvent.GetWaterColor`; handler de névoa de outros mods (Biomes O' Plenty, se estiver no pack).
* Blocos com `ISimpleBlockRenderingHandler` chamados no passe 1.
* LODs do DistantHorizons.

## Riscos

| Risco | Efeito | Mitigação |
|---|---|---|
| Cliente e Crucible têm bytecode diferente nas classes de fluido | mixin aplica num lado e falha no outro | impressão digital contra os dois; nada por ordinal em `BlockDynamicLiquid` |
| Mudança de "na água" altera a pesca ou o nado | regressão silenciosa (o Kunftig não exige as injeções) | telemetria; ligar por opção; a altura lógica não muda |
| Água hospedada espalha sem `BlockFromToEvent` | plugin de proteção fura | disparar o evento na rotina própria |
| Estouro do tampão de pacote do BelowCore | seções profundas param de ser enviadas | formato de 512 bytes; teste de tamanho |
| Versão nova do Angelica muda o `WorldSlice` | água de bloco waterlogged some sob o Angelica | mixin opcional com queda segura; a física não depende dele |
| Somar empurrão moderno ao do Et Futurum | entidades rápidas demais em correnteza | detectar e ceder |
| Gravidade moderna em mobs | fazendas e elevadores de água mudam | só jogador no início |
| Leituras diretas de material em mods de terceiros | bloco waterlogged parece seco para eles | aceitar; documentar; API para os nossos |
