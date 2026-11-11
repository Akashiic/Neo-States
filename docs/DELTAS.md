# Deltas 1.7.10 × 1.21.1

Cada linha compara o código lido dos dois lados ([VANILLA-1.7.10.md](VANILLA-1.7.10.md),
[MODERN-1.21.1.md](MODERN-1.21.1.md)). A regra do projeto é a mesma do Akashic Player Backport: **só se porta um delta
provado**. O que é algebricamente igual fica como está.

Colunas: **Igual?** diz se o comportamento observável muda. **Decisão** é a proposta da Fase 0.

**Decisões do dono na Fase 1** (valem sobre a coluna "Decisão" onde divergem):

* C-4, C-5, C-6 (empurrão do fluido): **portar exatamente como no 1.21.1**, com código nosso, sem contar com o Et
  Futurum.
* D-3, D-5, D-7 (gravidade, pulo e descida na água): **fiéis ao 1.21.1**, para toda entidade viva, não só o jogador.
* Empurrão **entre entidades** (seção I, nova): também fiel ao 1.21.1; ainda não levantado.

## A. Altura e geometria do fluido

| ID | Tema | 1.7.10 | 1.21.1 | Igual? | Decisão |
|---|---|---|---|---|---|
| A-1 | Altura por nível | superfície `1 − (decay+1)/9` | `amount/9`, `amount = 8 − decay` | **igual** (`(8−d)/9`) | não portar |
| A-2 | Fonte | `8/9` | `8/9` | **igual** | não portar |
| A-3 | Fluido em cima | render devolve `1` | `getHeight` devolve `1` | **igual** no render | não portar |
| A-4 | Queda | metadata `>= 8` tratado como `0` | `getFlowing(8, true)` → `8/9` | **igual** | não portar |
| A-5 | Altura para o corpo | teste contra inteiro, sempre verdadeiro | `topo >= box.minY` com a altura real | **difere** | portar com a detecção (C-1) |
| A-6 | Altura para os olhos | célula inteira (patch do Forge) | altura real | **difere** | portar: afeta névoa, respiração e nado |
| A-7 | Altura para a câmera | `1 − decay/9` (1/9 acima do desenho) | altura real | **difere** | portar no cliente |
| A-8 | Altura para o anzol | `1 − decay/8` | altura real | **difere** | não mexer: o Kunftig já corrige o anzol |
| A-9 | Altura dos cantos | média das 4 células; fonte pesa 10 | dois lados + diagonal condicional; `>= 0.8` pesa 10 | difere em quinas com parede | portar junto do waterlogging (o cálculo já vai passar pelo pipeline) |

Conclusão de A: a altura **lógica** não mudou. O que mudou foi que o 1.21.1 usa uma definição só; o 1.7.10 tem cinco
(seção 3 do documento vanilla). O ganho está em unificar as leituras, não em mudar a tabela de níveis.

## B. Vetor de fluxo

| ID | Tema | 1.7.10 | 1.21.1 | Igual? | Decisão |
|---|---|---|---|---|---|
| B-1 | Diferença entre vizinhos | `l1 − l` em decay | `(own − vizinho)` em nono | **igual** após `normalize` (fator `1/9`) | não portar |
| B-2 | Vizinho vazio sobre fluido | `l1 − (l − 8)` | `own − (abaixo − 8/9)` | **igual** | não portar |
| B-3 | Termo de queda | `normalize + (0, −6, 0)` com 8 testes | idem | **igual** na forma | não portar |
| B-4 | Parede no termo de queda | `material.isSolid()` | `isFaceSturdy` (forma) | difere em blocos parciais | portar só para blocos com forma conhecida **[A VERIFICAR]** |
| B-5 | Vizinho de outro fluido | entra no ramo "vazio" e olha embaixo | ignorado (`affectsFlow`) | difere (lava ao lado de água) | não portar (raro) |
| B-6 | Vizinho waterlogged | não existe | conta como fonte (`8/9`) | **novo** | portar com a Camada C |

Conclusão de B: `getFlowVector` e `FlowingFluid.getFlow` são a mesma função. Não há física de fluxo a "modernizar";
o delta real é o vizinho waterlogged (B-6).

## C. Entidades: detecção e empurrão

| ID | Tema | 1.7.10 | 1.21.1 | Igual? | Decisão |
|---|---|---|---|---|---|
| C-1 | Caixa de detecção | `expand(0, −0.4, 0).contract(0.001)` | `deflate(0.001)` | **difere** | portar para jogadores primeiro: conserta `inWater` com a caixa de 0.6 da pose de nado |
| C-2 | Critério | qualquer célula de água que cruza a caixa cortada | superfície `>=` base da caixa | **difere** nos dois sentidos | portar junto com C-1 |
| C-3 | Escala do empurrão | `0.014` | `0.014` | **igual** | não portar |
| C-4 | Combinação | soma, `normalize`, × 0.014 | média por célula; jogador **não** normalizado | **difere** | decisão do dono: o Et Futurum já altera este ponto (ver nota) |
| C-5 | Água rasa | sem escala | fluxo × altura se altura `< 0.4` | **difere** | junto com C-4 |
| C-6 | Empurrão mínimo | não há | `0.0045` se quase parado | **difere** | junto com C-4 |
| C-7 | Olhos | `isInsideOfMaterial` | `fluidOnEyes` com altura | ver A-6 | portar |
| C-8 | Lava empurra | não | sim (`0.00233…` / `0.007`) | **difere** | não portar: o Et Futurum já faz |

**Nota sobre C-4 a C-6**: o Et Futurum Requiem, com `adjustedLiquidPhysics` (padrão ligado), já injeta em
`World.handleMaterialAcceleration` (`@ModifyConstant` no `0.014D`, `@WrapOperation` em `Vec3.normalize`) e em
`BlockLiquid.velocityToAddToEntity`. É uma calibração própria dele, diferente da do 1.21.1. Portar a fórmula moderna
por cima somaria os dois efeitos. Ver [COMPATIBILIDADE.md](COMPATIBILIDADE.md#et-futurum-requiem).

## D. Entidades: movimento na água

| ID | Tema | 1.7.10 | 1.21.1 | Igual? | Decisão |
|---|---|---|---|---|---|
| D-1 | Arraste horizontal | `0.8` | `0.8`; `0.9` correndo | difere só correndo | **já portado** pelo Akashic Player Backport; não tocar |
| D-2 | Arraste vertical | `0.8` | `0.8` | **igual** | não portar |
| D-3 | Gravidade na água | `−0.02` por tick | `−0.005` (`g/16`), nada se correndo | **difere** | portar com flag, só jogador no início |
| D-4 | Afundamento terminal | `−0.1` bloco/tick | `−0.025` bloco/tick | **difere** (4×) | consequência de D-3 |
| D-5 | Pulo na água | `+0.04` por tick | `+0.04`, com regra de limiar `0.4` | forma quase igual | portar a regra do limiar com D-3 |
| D-6 | Subida terminal | `+0.1` | `+0.175` | **difere** | consequência de D-3 |
| D-7 | Descer agachado | não existe | `−0.04` por tick (`−0.225` terminal) | **novo** | portar com D-3 (sem ele, afundar fica lento demais) |
| D-8 | Aceleração | `0.02` (`0.04` com IA nova) | `0.02` + eficiência | difere para mobs | não portar |
| D-9 | Sair pela borda | `motionY = 0.3` se livre em `+0.6` | idem com `isFree` | **igual** | não portar |
| D-10 | Nado guiado pelo olhar | não existe | `Player.travel` | **novo** | **já portado** pelo Akashic Player Backport (`SwimmingVerticalHooks`) |
| D-11 | Itens flutuam | afundam | `y += 0.0005`, `x,z × 0.99` | **difere** | não portar: o Et Futurum já faz (`liquidItemFloat`) |
| D-12 | Depth Strider, Dolphin's Grace | não existem | atributo e efeito | **novo** | fora do escopo |

D-3 é o maior delta sentido pelo jogador e o de maior risco: muda a subida de elevadores de água, a queda em poços e
qualquer fazenda que conte com mobs afundando. Por isso a proposta é jogador primeiro, atrás de uma opção.

## E. Simulação de fluxo

| ID | Tema | 1.7.10 | 1.21.1 | Igual? | Decisão |
|---|---|---|---|---|---|
| E-1 | Intervalo de tick | 5 | 5 | **igual** | não portar |
| E-2 | Queda de nível | 1 | 1 | **igual** | não portar |
| E-3 | Busca de declive | 4 | 4 | **igual** (água) | não portar |
| E-4 | Fonte infinita | 2 fontes + sólido ou fonte embaixo | idem + gamerule `waterSourceConversion` | **igual** por padrão | não portar; o Crucible já tem a opção |
| E-5 | Par dinâmico/estático | dois blocos, ticks de bloco | um bloco, fila `fluidTicks` | arquitetura | não portar; a Camada C precisa de ticks próprios |
| E-6 | Espalhar para os lados ao descer | nunca | sim, com 3+ fontes vizinhas | **difere** | portar com flag **[A VERIFICAR o efeito em fazendas]** |
| E-7 | Nível escrito no vizinho | `decay + 1` | o que o vizinho calcularia | difere por 1 tick | não portar |
| E-8 | Passagem entre células | `material.blocksMovement()` | forma de colisão das faces | **difere** | portar só o caso waterlogged |
| E-9 | Fluxo contra bloco waterloggable | destrói ou é barrado pelo material | barrado, sem destruir | **difere** | portar com a Camada C |
| E-10 | Bloqueadores fixos | portas, placa de pé, escada, cana, portal | portas, placas (todas), escada, cana, coluna, portais | quase igual | não portar |
| E-11 | Lava × água | `l <= 4` → cobblestone | regra por fonte e direção | difere | fora do escopo (o Crucible já altera) |

## F. Waterlogging (tudo novo)

| ID | Regra moderna | Proposta |
|---|---|---|
| F-1 | Um bit por bloco; o fluido é sempre água fonte | camada de 1 bit por célula ([ADR-0001](adr/ADR-0001-armazenamento-waterlogging.md)) |
| F-2 | Colocar bloco em água fonte → nasce com água | gancho em `Chunk.func_150807_a` |
| F-3 | Balde cheio no bloco → enche; vazio → esvazia | `FillBucketEvent` |
| F-4 | Quebrar o bloco → sobra a água | gancho em `World.setBlockToAir` |
| F-5 | `setBlock(ar)` direto → a água some | limpar o bit em `Chunk.func_150807_a` |
| F-6 | Pistão remove a água do bloco movido | consequência de F-5 |
| F-7 | A água de dentro espalha como fonte | tick próprio, sem escrever na célula hospedeira |
| F-8 | Conta para a regra das 2 fontes | leitura de vizinho pelo pipeline |
| F-9 | Entidade dentro conta como na água | costuras de leitura (ADR-0001, seção "Leitura") |
| F-10 | Face da água oculta pela forma do bloco | sem formas de oclusão no 1.7.10: recuo de `0.001` do render vanilla |
| F-11 | Tile entity intacto | invariante: a Camada C só escreve o bit |

## G. Visual

| ID | Tema | 1.7.10 | 1.21.1 | Decisão |
|---|---|---|---|---|
| G-1 | Textura | azul (`#2F43F4`), alfa 168 a 226 | cinza (`#B1B1B1`), alfa 180 | cinza gerado em tempo de execução ([ADR-0002](adr/ADR-0002-interceptacao-visual.md)) |
| G-2 | Cor | multiplicador branco; pântano `#E0FFAE` | `water_color` por bioma, padrão `#3F76E4` | `BiomeEvent.GetWaterColor` |
| G-3 | Blend entre biomas | média 3×3 fixa | raio configurável | já existe no Angelica; vanilla fica com 3×3 |
| G-4 | Cor da névoa | `(0.02, 0.02, 0.2)` + respiração | `water_fog_color` com transição de 5 s | `EntityViewRenderEvent.FogColors` |
| G-5 | Forma da névoa | `GL_EXP` densidade `0.1` | linear `−8 .. 96 × visão` | `EntityViewRenderEvent.FogDensity` |
| G-6 | Adaptação da visão | não existe | `waterVisionTime` (600 ticks) | contador no cliente |
| G-7 | Névoa mais próxima | não existe | `× 0.85` em pântanos | tabela por bioma |
| G-8 | `water_overlay` em vidro e folhas | não existe | sim | portar depois **[A VERIFICAR com o Angelica]** |
| G-9 | Verso do topo | sempre uma face | `shouldRenderBackwardUpFace` | portar depois |
| G-10 | FOV submerso | `× 60/70` | escalado por `fovEffectScale` | não portar |
| G-11 | Luz dentro d'água | a água tira 3 de luz por bloco | tira 1 | portar: visto no teste do pack ([TESTES.md](TESTES.md)); o Hodgepodge já tem `useLighterWater` |

## H. Coluna de bolhas

O Et Futurum Requiem já tem `BlockBubbleColumn` (padrão ligado), com soul sand para cima e magma para baixo. A proposta
é **não reimplementar** e só garantir que as costuras do Neo States tratam esse bloco como água. Comparar as constantes
dele com as do 1.21.1 (tabela na seção 8 do documento moderno) fica **[A VERIFICAR]**.

## I. Empurrão entre entidades (a levantar)

Pedido do dono na Fase 1: colisão entre entidades e entre jogadores como no 1.21.1. É outro assunto que o empurrão do
**fluido** (seção C): no 1.21.1 são `Entity.push(Entity)`, `LivingEntity.pushEntities` e a gamerule
`maxEntityCramming`; no 1.7.10, `Entity.applyEntityCollision` e `EntityLivingBase.collideWithNearbyEntities`.

Nada disso foi lido na Fase 0. Dois fatos já conhecidos condicionam o trabalho:

* o Akashic Player Backport já tem um transformador em `Entity.applyEntityCollision` (`PlayerCollisionTransformer`) e
  regras de colisão próprias (`CollisionPolicy`, `CollisionRuleSavedData`);
* o Hodgepodge tira as caixas de entidade da colisão de `EntityItem` (`fastItemEntityPhysics`).

Decisão do dono: isso mora no **Akashic Player Backport**; o Neo States pode se ligar a ele se precisar.
**[A VERIFICAR]** os dois lados no decompilador, lá.

## Resumo: o que vale portar

1. Unificar a leitura de altura (A-5, A-6, A-7, C-1, C-2).
2. Waterlogging inteiro (F), com B-6, E-8 e E-9.
3. Visual (G-1 a G-7).
4. Gravidade e pulo na água do jogador (D-3, D-5, D-7), atrás de opção.
5. Empurrão do fluido como no 1.21.1 (C-4 a C-6), decidido na Fase 1. Falta decidir E-6.

O que **não** se porta: o motor `FlowingFluid` inteiro, a tabela de níveis, o vetor de fluxo, o arraste (já é do
Player Backport), bolhas e itens flutuando (já são do Et Futurum).

### Estado (Fase 6)

Portados e ligados por `modernWaterPhysics`: A-5, A-6, A-7, C-1, C-2, C-4, C-5, C-6, D-3, D-5, D-7. Portado e ligado
por `modernWaterLight`: G-11. Continuam de fora: B-4, E-6, E-8 (além do caso hospedado), G-8, G-9.

G-11 tem duas partes. A opacidade muda o que é calculado dali em diante; a luz **guardada** nos chunks já gerados só
muda onde um bloco muda, então um mundo antigo ficaria escuro debaixo d'água para sempre. Por isso cada chunk antigo
tem a luz da água refeita uma vez, ao carregar (`relightOldChunks`). Só clareia: desligar `modernWaterLight` depois
não escurece o que já foi refeito. Um bloco com água (cerca, painel) não ganha a opacidade 1 que tem no 1.21.1: fica
com a do próprio bloco; lajes e escadas do 1.7.10 já barram a luz inteira, com ou sem água.

Na Fase 7 entrou a regra das fontes novas para blocos que contêm água (`FlowingFluid.getNewLiquid`): entre duas
fontes, sobre bloco sólido ou fonte, o bloco enche. O 1.21.1 também olha a forma do bloco para decidir se a água
passa por uma face; aqui a forma não é considerada.

O empurrão de um jogador depende de a física rodar no cliente (no 1.7.10 o cliente é quem move o jogador): a opção
tem de ter o mesmo valor nos dois lados.

### Como o empurrão fiel convive com o Et Futurum

O Et Futurum (`adjustedLiquidPhysics`, ligado por padrão) injeta **dentro** de `World.handleMaterialAcceleration`.
Para o resultado ser exatamente o do 1.21.1, o Neo States não pode passar por esse método para a água: a detecção e o
empurrão passam a ser calculados por código nosso a partir de `Entity.handleWaterMovement` (equivalente a
`updateFluidHeightAndDoFluidPushing`). Com isso as injeções do Et Futurum na água ficam sem efeito, sem conflito de
mixin; a parte dele que faz a **lava** empurrar continua valendo. Recomendação para o pack: desligar
`adjustedLiquidPhysics` para não ter dois donos do mesmo comportamento.

A impressão digital do Player Backport não é tocada: a gravidade troca a constante `0.02D`, não os três `0.8`.
