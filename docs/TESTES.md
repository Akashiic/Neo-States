# Testes no pack real

Registro do que foi executado, onde e com que resultado. O que não foi executado fica dito.

## Ambiente

Cópia isolada, num contêiner Linux sem placa de vídeo (renderização por software, tela virtual):

| Peça | O que foi usado |
|---|---|
| Cliente | TEOF 5.4 (`TEOF-5.4.zip`), 215 mods, Java 8, Angelica 2.2.43, **sem shaders** |
| Servidor | os arquivos do servidor do dono, Crucible `1.7.10-staging-67b53034`, Java 21 |
| Neo States | `0.1.0-alpha.2` nos dois lados |

Versões confirmadas no pack: Angelica 2.2.43, Hodgepodge 2.7.219, Et Futurum Requiem 2.6.64-GTNH, GTNHLib 0.11.54,
ChunkAPI 0.8.4, EndlessIDs 1.7.4 com `extendBlockItem=true`, ArchaicFix 0.8.0 (só no cliente). Não há FluidLogged.

Adaptações feitas **só na cópia de teste** do servidor:

* todos os plugins desligados, não só o CastleOfStone e o BungeeGuard: vários falam com serviços externos (Discord,
  banco de dados, pagamentos) e o teste não pode tocar neles;
* `bungeecord: false` no `spigot.yml`, lista branca vazia, mundo novo;
* mods trocados pelas versões do cliente 5.4: AM2.5, ATM-Family, Eln, Omni Pipes, Adventure Backpack, AE2, Et Futurum,
  GTNHLib, Hodgepodge, akashicmixer, Akashic Player, Baubles Expanded, DistantHorizons (Mirai);
* mods acrescentados do cliente: Akashic Bellow, VitaGenesis, Kunftig, 3D Futura Engine;
* `akashicweb` removido: registra um bloco que o cliente 5.4 não tem, e a conexão é recusada;
* `Arc Furnace: Recycling=false` no Immersive Engineering: o perfil de receitas dele estoura 11 GB de heap ao iniciar
  nesta máquina (15 GB no total).

Essas diferenças entre os arquivos do servidor e o cliente 5.4 existem independentemente do Neo States.

## Fases 1 e 2 (2026-11-11)

| Verificação | Resultado |
|---|---|
| Crucible sobe com os mods do pack e o Neo States | passou: `Done`, estado `UCHIJAAAA`, comando `/neostates` registrado |
| Cliente do pack sobe com o Neo States | passou: menu principal; o mixin de `BlockLiquid` aplica; os dois sprites em cinza carregam (16×512 e 32×1024, 32 quadros) |
| Cliente entra no servidor | passou |
| Água na superfície com o Angelica | passou: textura em cinza tingida, animada, translúcida ([imagem](evidencias/fase-2/agua-superficie-angelica.jpg)) |
| Névoa submersa | passou: escura ao entrar ([imagem](evidencias/fase-2/submerso-ao-entrar.jpg)); depois de ~45 s a visão abre e o lago de 40 blocos aparece inteiro ([imagem](evidencias/fase-2/submerso-visao-adaptada.jpg)) |
| `/neostates sonda` no Crucible | passou: `pés y=59 água nível 8 fonte superfície=60.0000`, `olhos y=60 … superfície=61.0000`, `na água: vanilla=true 1.21.1=true` |

Achados:

1. **O ícone da água em cinza aparece sem cor no WAILA** (e deve aparecer assim em qualquer interface que desenhe
   `Blocks.water.getIcon` sem `colorMultiplier`). Cosmético; a corrigir.
2. **Debaixo d'água fica escuro.** O bloco de água do 1.7.10 tira 3 de luz por bloco; o do 1.21.1 tira 1. É um delta
   que não estava em [DELTAS.md](DELTAS.md) (entra como G-11). O Hodgepodge já tem a opção `useLighterWater`
   (desligada por padrão) que faz exatamente isso.
3. A névoa adaptada é um azul bem saturado. É o que a fórmula do 1.21.1 produz com a cor `#050533` (o clareamento leva
   o canal mais forte a 1), mas no 1.21.1 o fundo é mais claro por causa do item 2. Rever junto com ele.

## Fase 3 (2026-11-11): dados do waterlogging

Mesmo ambiente, Neo States `0.1.0-alpha.3`. A camada ainda não muda o jogo: só guarda, salva e sincroniza o bit, e o
comando `/neostates agua <x> <y> <z> <on|off>` o escreve à mão.

| Verificação | Resultado |
|---|---|
| Campo na `ExtendedBlockStorage` junto dos mixins do EndlessIDs, no Crucible e no cliente | passou: os dois lados sobem |
| Ligar a água hospedada num baú | passou: `bloco=minecraft:chest meta=3 tile=TileEntityChest hospedada=true`; bloco, metadata e TileEntity iguais antes e depois |
| Ligar numa laje | passou |
| Ligar numa laje em **Y −5** (seção profunda do BelowCore) | passou |
| Persistência: parar e subir o Crucible | passou: as três células voltam com `hospedada=true`, a de Y −5 inclusive |
| Camada do chunk chega ao cliente | passou: `Waterlogging layer of chunk -15,17: 2 sections` (a seção 4 e a seção −1) |
| Mudança de uma célula chega ao cliente | passou: `Waterlogging at -240 -5 285: false`, `… 71 285: true` |

Isso fecha quatro verificações do [ADR-0001](adr/ADR-0001-armazenamento-waterlogging.md): o campo convive com o
EndlessIDs; `ChunkDataEvent.Load` dispara depois de o BelowCore recriar as seções profundas; `ChunkDataEvent.Save` e
`Load` funcionam no Crucible; a seção profunda chega ao cliente antes de a camada desistir de esperar.

Limites desta fase, de propósito:

* o bit não é limpo quando o bloco muda (isso é a Fase 4);
* nada é desenhado nem afeta entidades (Fases 4 e 5);
* `ChunkCache` e o `WorldSlice` do Angelica ainda não enxergam a camada.

Não executado nesta fase: o caminho de reenvio de chunk do Hodgepodge, troca de dimensão, e o proxy.

## Fase 4 (2026-11-11): o waterlogging passa a valer

Neo States `0.1.0-alpha.4`. A água hospedada ainda **não é desenhada** (Fase 5); o que foi testado é o comportamento.

`/neostates autoteste` monta uma plataforma de pedra na área de spawn e confere 30 regras dentro do servidor. Rodou no
servidor de desenvolvimento e no **Crucible real com o pack inteiro**: `RESULTADO: PASSOU` (30 de 30) nos dois.

| Grupo | Regras conferidas |
|---|---|
| API | laje aceita água; `fluidAt` diz fonte hospedada; pedra recusa |
| Consultas do mundo | `isAnyLiquid`, `isMaterialInBB` (água sim, lava não), `isAABBInMaterial`, `handleMaterialAcceleration`, `isInsideOfMaterial` veem a água hospedada, e deixam de ver sem o bit |
| TileEntity | baú com item: mesma TileEntity, NBT idêntico, bloco e metadata intactos com água |
| Colocar e remover | laje posta em água fonte nasce com água; em água corrente fica seca; bloco cheio tira a água; `setBlock(ar)` remove tudo; quebrar (`setBlockToAir`) deixa água fonte |
| Balde | balde de água enche uma escada; balde vazio tira; em bloco comum vale a regra vanilla |
| Espalhar | fonte hospedada sobre o vazio cai; no chão vai para os lados; o hospedeiro não é alterado; a água vizinha continua alimentada e seca quando a fonte some |

No cliente do pack (Angelica), conectado ao Crucible, com o jogador dentro de uma laje e de uma escada de mão com
água: `/neostates sonda` respondeu `pés … água nível 8 fonte hospedada`, `na água: vanilla=true`,
`olhos na água: vanilla=true`, e a câmera ganhou a névoa submersa
([imagem](evidencias/fase-4/camera-em-agua-hospedada.jpg)). A água da escada caiu para a célula de baixo sozinha, como
uma fonte.

Isso também confere, no bytecode real do Crucible, os alvos de `Chunk.func_150807_a`, `World.setBlockToAir`,
`notifyBlockOfNeighborChange`, as quatro consultas de fluido de `World`, `Entity.isInsideOfMaterial`,
`BlockLiquid.func_149804_e` / `getEffectiveFlowDecay` e os três métodos privados de `BlockDynamicLiquid`: todas as
injeções são obrigatórias e o servidor subiu.

O que esta fase **não** faz ou não testou:

* **`BlockFromToEvent` não é disparado** quando a água hospedada espalha. Com um plugin de proteção, esse fluxo
  atravessaria regiões protegidas. Tem de entrar antes de usar em produção.
* A fila de ticks da água hospedada não é salva: depois de reiniciar, a fonte só volta a correr na próxima mudança
  de vizinho.
* A regra das 2 fontes não enche um bloco hospedeiro (no 1.21.1 enche).
* Trilhos ficam de fora (a água corrente do 1.7.10 os quebra).
* Balde e quebrar foram conferidos chamando as regras por código, não com um jogador clicando.
* O Player Backport e o Kunftig ainda leem o bloco direto: o **início** do nado dentro de água hospedada e o anzol
  não a veem até passarem a usar a API.
* `ChunkCache` e o Angelica não enxergam a camada: nada é desenhado.

## Fase 5 (2026-11-11): desenhar a água hospedada

Neo States `0.1.0-alpha.5`, cliente TEOF 5.4 com Angelica 2.2.43 (sem shaders) no Crucible.

| Verificação | Resultado |
|---|---|
| Autoteste das 30 regras no Crucible com a versão nova | passou |
| Laje (de baixo e de cima), escada, cerca, painel de vidro, muro e baú colocados dentro de um lago | todos `hospedada=true`; a superfície do lago fica contínua em volta deles e os blocos aparecem por baixo da água ([imagem](evidencias/fase-5/blocos-com-agua-no-lago-angelica.jpg)) |
| Os mesmos blocos numa prateleira no ar, com água ligada à mão | a água é desenhada em volta de cada um e cai em cascata ([imagem](evidencias/fase-5/prateleira-com-cascata-angelica.jpg)) |
| A camada chega às threads de meshing do Angelica | sim: os dois mixins tardios (`ExtendedBlockStorageExt.copyFrom`, `WorldSlice`) aplicam e o desenho acompanha as mudanças |

Duas injeções obrigatórias falharam no pack na primeira tentativa, e o cliente **não subiu** (é a falha fechada):

1. `BlockLiquid.shouldSideBeRendered`: no pack a leitura de bloco dentro do método já não é a chamada vanilla. Passou
   a ajustar o **resultado** do método, que não depende do corpo.
2. `WorldRenderer.updateRenderer`: o Angelica reescreve a chamada que o gancho envolve, num método que ele nem usa. O
   gancho agora só é carregado sem o Angelica.

Fica a lição para as próximas costuras de cliente: o corpo dos métodos de render no pack não é o do vanilla; preferir
ganchos de entrada, de saída ou de resultado.

Não executado nesta fase:

* **cliente sem Angelica dentro de um mundo**: o cliente de desenvolvimento sobe até o menu com os mixins carregados,
  mas o gancho de `WorldRenderer` só é aplicado quando o mundo abre. O desenho no vanilla puro continua
  **[A VERIFICAR]**;
* shaders: a água hospedada é marcada como água para o shaderpack (`Iris.setShaderMaterialOverride`), sem teste;
* blocos de mods que sobrescrevem `canRenderInPass` (a água deles não é desenhada, por desenho);
* ordenação de translúcidos com vários blocos com água em fila.

## Fase 6 e pendências (2026-11-11)

Neo States `0.1.0-alpha.6`, mais duas builds candidatas dos outros repositórios com a ponte para o Neo States:
Akashic Player `0.1.0-alpha.30-neostates-bridge` e Kunftig `0.1.0-dev-neostates`. As três nos dois lados.

O autoteste ganhou 5 verificações (35 no Crucible, 34 no servidor de desenvolvimento, que não tem Bukkit):

| Verificação | Resultado |
|---|---|
| As 30 regras da Fase 4 | passou nos dois |
| O fluxo da água hospedada dispara `BlockFromToEvent` (Crucible) | passou: o evento chega ao Bukkit |
| Olho a 0.5 de uma fonte: dentro; a 0.95: fora (a superfície é 8/9) | passou |
| Uma entidade viva sem IA afunda a 0.025 bloco/tick num poço fechado | passou (`-0.0250`) |
| A mesma, segurando o pulo, sobe a 0.175 bloco/tick | passou (`0.1750`) |

No pack:

| Verificação | Resultado |
|---|---|
| O contrato do Player Backport em `moveEntityWithHeading` | mantido: `Installed swimming horizontal drag hooks … (2 axes)` no servidor e no cliente, sem `Refusing swim movement` |
| O Player Backport enxerga o Neo States | `Neo States found: water held inside blocks counts for swimming` |
| Ícone da água no WAILA | voltou a ser o azul ([imagem](evidencias/fase-6/submerso-icone-azul-luz-11.jpg)) |
| Luz dentro d'água | o WAILA mostra luz de céu 11 no ponto do lago que antes dava 6 |
| Cliente e servidor sobem com as três builds novas, jogador entra no lago, sonda responde | passou |

Fora do Neo States: o log do servidor tem `AkashicWorldFirewallCore: Refusing WorldServer block tick patch:
originalCalls=0, hookCalls=0`. Essa linha já aparecia no primeiro boot, com o Neo States `alpha.2` (sem nenhum mixin
no servidor), então não vem daqui; mas é um patch daquele mod que não está sendo aplicado no Crucible.

Não executado:

* **o evento de fluxo com um plugin de proteção de verdade**: confirmei que o evento é entregue ao Bukkit, não que um
  plugin o cancela e a água para (os plugins estão desligados na cópia de teste);
* **nadar de verdade no cliente**: correr e nadar dentro de água hospedada, descer agachado, pular no raso. O ambiente
  não tem controle de mouse; as velocidades foram medidas no servidor com uma entidade de teste;
* **pesca**: o anzol do Kunftig com a ponte não foi lançado;
* o empurrão da correnteza num jogador (a fórmula tem teste unitário; em jogo, não).

## Fase 7 e pendências da Fase 6 (2026-11-11)

Neo States `0.1.0-alpha.7`, Akashic Player `0.1.0-alpha.30-push-rc1` (candidata) e Kunftig `0.1.0-dev-neostates`, nos
dois lados, no TEOF 5.4 com o Crucible.

### Luz dos chunks antigos

| Verificação | Resultado |
|---|---|
| Algoritmo contra o cálculo do zero (teste unitário, 60 mundos aleatórios com cavernas, água e lâmpadas) | idêntico |
| Autoteste: coluna de água com a luz de um mundo antigo (3 por bloco) refeita | `14,13,12,11,10,9` do topo, céu 15 |
| Autoteste: a luz entra de lado numa célula de água vizinha | passou (9) |
| Primeiro boot com o mundo do servidor | 529 chunks refeitos, 120.406 células clareadas, 363 ms no total (0,7 ms por chunk) |
| Segundo boot | 0 refeitos na área do spawn: a marca `NeoStates.Light` foi salva e lida |
| Cliente que já tem o chunk recebe a luz nova | `Relit water light from the server` no log do cliente |
| Fundo do lago | luz 11 a 4 blocos de profundidade ([imagem](evidencias/fase-7/luz-fundo-do-lago.jpg)) |

O anel mais externo de chunks carregados em volta de um jogador parado fica na fila: um chunk só é refeito com os oito
vizinhos carregados. É refeito quando o jogador chega mais perto.

### Opções do servidor no cliente

`Following the server: water physics true, water light true, waterlogging true.` no log do cliente ao entrar.
Servidor sem o Neo States (as três desligadas no cliente): **não testado**.

### Física com um jogador de verdade

Medido no servidor com `/neostates rastro` (as posições que o cliente manda), teclado pelo `xdotool`. O cliente roda
em renderização por software e os pacotes de posição não vêm um por tick, então são médias:

| Movimento | Medido | 1.21.1 | 1.7.10 |
|---|---|---|---|
| Parado, afundando | 0,026 por tick | 0,025 | 0,100 |
| Segurando o pulo | ~0,15 por tick | 0,135 | 0,135 |
| Agachado | ~0,18 por tick | 0,185 | 0,100 |

Nadar correndo, pulo no raso e empurrão da correnteza num jogador: **não testados**.

### Fluxo e plugin

Com um plugin de teste que cancela o `BlockFromToEvent` (fora do repositório): a água posta numa laje tentou os quatro
lados, os quatro eventos foram cancelados e nada escorreu; com o plugin desligado, a mesma laje escorreu. Um plugin de
proteção de verdade (WorldGuard e afins): **não testado**.

Autoteste: laje com uma fonte ao lado fica seca; entre duas fontes, sobre bloco sólido, enche.

### Pesca

Vara vanilla, anzol do Kunftig: em água normal e sobre lajes com água, a boia fica na superfície
([imagem](evidencias/fase-7/pesca-boia-na-superficie.jpg)). Mordida, fisgada e o AkashicFishing: **não testados**.

### Colisão (Akashic Player Backport)

Com o jogador parado no chão, um porco invocado em cima dele o empurrou 0,34 bloco para o lado oposto em 7 ticks, até
se separarem. Antes, no 1.7.10, o jogador não se movia. Jogador contra jogador (precisa de dois clientes) e as regras
de time: **não testados** em jogo.

### Autoteste

40 verificações, `RESULTADO: PASSOU` no Crucible, em dois lugares do mundo.

## Fase 8: fechamento (2026-11-11)

Neo States `0.1.0-alpha.8`. O plano original ia até a Fase 7; esta fase fecha o que vinha ficando como "não testado"
e uma diferença de comportamento que faltava (a forma dos blocos que contêm água).

### Forma dos blocos (1.21.1 `canPassThroughWall`)

Autoteste: laje de baixo com água não vaza para baixo e escorre para os lados; laje de cima vaza para baixo; escada
não solta água pelas costas e solta pela frente. A regra das duas fontes também respeita as faces fechadas.

### Um erro achado pelo teste com jogador

O pulo no raso não funcionava **para o jogador local**: no 1.7.10 o `getEyeHeight()` do jogador no cliente devolve
0,12 (a altura fica no `posY`), e a regra do 1.21.1 trata olho abaixo de 0,4 como "sem limiar". O autoteste da Fase 6
não pegava porque a entidade de teste tem olho normal. Corrigido (altura do olho acima dos pés) e coberto por uma
entidade de teste montada como o jogador do cliente.

### Física com um jogador de verdade (TEOF 5.4 + Crucible, `/neostates rastro`)

| Movimento | Medido | 1.21.1 |
|---|---|---|
| Parado numa correnteza reta (canal, nível 6) | 0,070 por tick em regime | 0,014 ÷ 0,2 = 0,070 |
| Pulo em água de nível 1 (altura 0,11), pé no chão, antes da correção | pico 0,36 | pulo normal |
| O mesmo, depois da correção | 161,42 → 161,75 → 162,00 → 162,16, pico 1,24 | 0,42; 0,751; 0,997; 1,16... |
| Pulo no seco, ao lado (referência) | pico 1,249 | 1,249 |
| Nadando para a frente, submerso | afunda 0,024 por tick | 0,025 |
| Nadando correndo (Ctrl), submerso, olhando para a frente | não afunda (0,000), mais rápido | sem gravidade ao correr |

A correnteza reta dá o mesmo número no 1.7.10; serve para ver que o empurrão novo não quebrou, não para distinguir.

### Sem Angelica e cor por bioma

Cliente de desenvolvimento (só Forge, UniMixins e GTNHLib, sem Angelica) contra o servidor de desenvolvimento, mundo
plano de pântano: a água sai com a cor do pântano e os blocos com água (muro, painel, cerca, escada, laje) são
desenhados dentro dela ([imagem](evidencias/fase-8/sem-angelica-pantano.jpg)). Autoteste nesse ambiente: 43
verificações (sem as do Bukkit).

### Autoteste

46 verificações, `RESULTADO: PASSOU` no Crucible.

## Fase 9: itens, orbes, ar e trilhos (2026-11-11)

Neo States `0.1.0-alpha.9`. Tabela do que foi portado em [DELTAS.md](DELTAS.md), "Fase 9".

### O que os testes pegaram antes de funcionar

Quatro tentativas até o item boiar no Crucible; cada falha foi lida, não contornada:

| Sintoma | Causa | Onde apareceu |
|---|---|---|
| Servidor recusa subir: `@Shadow method func_70086_ai ... was not located` | `getAir()` é de `Entity`, não de `EntityLivingBase` | Crucible, na inicialização |
| Item parado no fundo, altura da água 0 | `EntityItem` tem `handleWaterMovement` próprio, fora da leitura do 1.21.1 | autoteste, desenvolvimento |
| Servidor recusa subir: `Constant modifier ... failed injection check` | o Galacticraft já modifica a constante da gravidade do item | Crucible, na inicialização |
| Item parado no fundo, altura da água 1 | gancho antes da chamada de `moveEntity`: os argumentos já estavam na pilha | autoteste, Crucible |
| Item sobe, mas o 1º tick dá 0,0495 | o Et Futurum agia no tick de entrada na água | autoteste, Crucible (diagnóstico dos três primeiros ticks) |

O bytecode real do Crucible foi lido com `-Dmixin.debug.export=true` (ver [COMPATIBILIDADE.md](COMPATIBILIDADE.md),
"Bytecode de verdade").

### Autoteste

52 verificações, `RESULTADO: PASSOU` no Crucible com o Et Futurum (`liquidItemFloat=true`) e o Galacticraft; 49 no
servidor de desenvolvimento. As novas:

| Verificação | Resultado no Crucible |
|---|---|
| Item boia a 0,0245 por tick | 0,0233 aos 150 ticks, subindo |
| Nenhum outro mod soma à flutuação do item | primeiros ticks `0.0005 0.0010 0.0014` |
| Orbe de experiência boia a 0,0245 por tick | 0,0233 |
| Fora d'água o ar volta 4 por tick e para em 300 | `100 -> 104 -> 300` |
| Água corrente não entra na célula de um trilho | passou |
| Trilho aceita água | passou |

### No cliente do pack

| Verificação | Resultado |
|---|---|
| Cliente sobe com os mixins novos (com Galacticraft, Et Futurum, PlayerAPI, Angelica) | passou |
| Maçãs douradas largadas na água | ficam na superfície ([imagem](evidencias/fase-9/item-boiando.jpg)) |
| Barra de ar depois de ficar sem ar e sair da água | 1, 2, 4, 6 e 7 bolhas em 0,7 s, 1,2 s, 1,7 s, 2,2 s e 2,8 s, fora d'água ([imagem](evidencias/fase-9/ar-voltando-fora-da-agua.png)): perto de 4 de ar por tick |

Não testado em jogo: item levado por uma correnteza, orbe na água com jogador por perto (o jogador o atrai), trilho
com água desenhado e com carrinho passando.

## Fase 10: entrega (2026-11-11)

Neo States `0.1.0-alpha.10`, com as mesmas builds do Player Backport e do Kunftig. Os dois plugins de teste estão em
`tools/testplugins/`.

| Verificação | Resultado |
|---|---|
| Autoteste no Crucible | 54 verificações, `RESULTADO: PASSOU` |
| Flecha na água guarda 0,6 do movimento | `-1,000 -> -0,650` num tick (0,6 e a gravidade de 0,05) |
| Item boiando é levado pela correnteza (autoteste, canal com níveis caindo) | andou 4,75 blocos em 30 ticks, pico 0,258 por tick |
| Pesca em água normal (vara vanilla, anzol do Kunftig, cliente do pack) | `FISHING`, `LURED`, `BITE`, `CAUGHT_FISH` |
| Pesca com o anzol sobre laje com água | os mesmos quatro eventos, anzol em Y 63,90 (a superfície é 63,89), bloco `STEP` |
| Trilho com água, carrinho ao lado | o trilho fica, a água é desenhada na célula dele ([imagem](evidencias/fase-10/trilho-com-agua.jpg)) |
| Cliente do pack sobe com a build | passou |

Tentei medir o item na correnteza em jogo com funis sob o canal e `/testforblock`; a consulta de NBT do 1.7.10 exige
a lista de itens idêntica e o método não deu leitura confiável, então a medida ficou no autoteste (mesma física, no
servidor).

Não testado: carrinho andando por um trilho com água; o AkashicFishing por cima do Kunftig.

## Fase 11: `water_overlay` (2026-11-12)

Neo States `0.1.0-alpha.11`. Um aquário: água atrás de uma parede de vidro, com uma coluna de gelo e uma de folhas.

| Verificação | Resultado |
|---|---|
| Sem Angelica (cliente de desenvolvimento), opção desligada e ligada | desligada: a água atrás do vidro tem as listras da textura corrente; ligada: lisa ([imagem](evidencias/fase-11/overlay-desligado-e-ligado.jpg)) |
| Com Angelica (cliente do TEOF 5.4 contra o Crucible) | o cliente sobe com o mixin novo, sem falha de injeção; a água atrás do vidro sai lisa ([imagem](evidencias/fase-11/overlay-com-angelica.jpg)) |
| Regra de onde a textura troca (teste unitário) | passou |

Não testado: com shader; contra folhas e gelo de perto (estão na cena, mas a captura mostra o vidro); vidro de mods
que não estendem `BlockBreakable` (ficam com a textura corrente).

## Não executado até aqui

Das Fases 1 e 2:

* **shaders** (BSL e Complementary estão no pack): precisam de placa de vídeo de verdade para um resultado que valha;
* biomas frios e de mods com cor própria (pântano e cor padrão foram vistos);
* os plugins do servidor e o proxy (Velocity + ProtocolObelisk).
