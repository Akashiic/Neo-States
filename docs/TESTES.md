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

Não executado:

* **shaders** (BSL e Complementary estão no pack): precisam de placa de vídeo de verdade para um resultado que valha;
* biomas diferentes (pântano, frio): o teste foi num bioma só, de mod (Ender Forest, cor padrão);
* cliente vanilla sem Angelica dentro de um mundo (só até o menu, no ambiente de desenvolvimento);
* os plugins do servidor e o proxy (Velocity + ProtocolObelisk).
