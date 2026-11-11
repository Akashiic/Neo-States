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

## Não executado até aqui

Das Fases 1 e 2:

* **shaders** (BSL e Complementary estão no pack): precisam de placa de vídeo de verdade para um resultado que valha;
* biomas diferentes (pântano, frio): o teste foi num bioma só, de mod (Ender Forest, cor padrão);
* cliente vanilla sem Angelica dentro de um mundo (só até o menu, no ambiente de desenvolvimento);
* os plugins do servidor e o proxy (Velocity + ProtocolObelisk).
