# Entrega: o que instalar e o que falta decidir

Estado em 2026-11-11, ao fim da Fase 10. Nada aqui foi publicado como release nem instalado no servidor de produção.

## O que vai junto

| Projeto | Branch | Build testada | Onde instalar |
|---|---|---|---|
| Neo States | `claude/friendly-edison-abcr7m` | `0.1.0-alpha.11` | cliente e servidor |
| Akashic Player Backport | `claude/friendly-edison-abcr7m` | `0.1.0-alpha.30-push-rc1` (candidata) | cliente e servidor |
| Kunftig | `claude/friendly-edison-abcr7m` | `0.1.0-dev-neostates` | cliente e servidor |

As três foram testadas juntas no TEOF 5.4 com o Crucible `1.7.10-staging-67b53034`. O Player Backport e o Kunftig
funcionam sem o Neo States (as pontes são por reflexão); o Neo States funciona sem eles.

## Como obter os jars

* **Neo States**: cada push roda o workflow `Build` do GitHub Actions, que publica `NeoStates-<versão>.jar` e o
  `-api.jar` como artefatos da execução. **[A VERIFICAR]**: o workflow foi escrito nesta fase e ainda não tinha
  rodado quando este documento foi escrito; confira a aba Actions.
* Localmente, em qualquer um dos três: `VERSION=<versão> ./gradlew build -x javadoc` (o Player Backport sem o
  `-x javadoc`). Um clone limpo do Neo States compila sem nada ao lado.

## Configuração

`config/neostates.cfg` nasce com tudo ligado. As opções estão no [README](../README.md#configuração). O valor do
servidor vale para os clientes conectados nas opções que os dois lados precisam ter iguais.

Não é preciso mexer na config de outros mods. Com as opções padrão:

* o empurrão de líquidos do Et Futurum (`adjustedLiquidPhysics`) deixa de valer para a água;
* a flutuação de itens do Et Futurum (`liquidItemFloat`) deixa de valer na água;
* a gravidade de itens do Galacticraft continua valendo fora d'água.

## Antes de pôr em produção

Isto não pôde ser verificado neste ambiente e fica com o dono do projeto:

1. **Mundo de produção**: no primeiro carregamento de cada chunk antigo a luz da água é refeita. No mundo de teste
   foram 0,7 ms por chunk. Faça um backup antes; a mudança de luz só clareia e não é desfeita desligando a opção.
2. **Plugins de proteção** de verdade (o teste usou um plugin mínimo que cancela o `BlockFromToEvent`).
3. **Proxy** (Velocity + ProtocolObelisk): o Neo States usa um canal próprio (`neostates`) com quatro mensagens.
4. **Shaders** (BSL, Complementary): precisam de placa de vídeo.
5. **AkashicFishing** por cima do Kunftig em água dentro de blocos.
6. **Player Backport**: a aceitação em jogo, e o travamento do agachar da alpha.21. A candidata muda a tabela de
   regras de time para a que o 1.21.1 aplica (`pushOwnTeam` e `pushOtherTeams` fazem o contrário do nome).

## O que não foi portado, e por quê

| Item | Motivo |
|---|---|
| Espalhar para os lados ao cair com 3 fontes vizinhas; parede no termo de queda | mexem no `BlockDynamicLiquid.updateTick`, que o Crucible reescreve |
| Omitir o verso do topo da água sob teto todo sólido | o 1.7.10 sempre desenha o verso; tirar pede um corte por ordinal num método que o Angelica também transforma, por uma diferença quase invisível |
| Opacidade 1 de um bloco com água | o gancho ficaria no caminho quente da luz e mexeria no mapa de altura durante a carga do chunk |
| Cantos de escada na forma dos blocos | o 1.7.10 calcula o canto pelos vizinhos; tratados como escada reta |
| Item boiando na lava | fica o do Et Futurum |
| Barcos | reescritos no 1.9; outro projeto |
| Dano por aglomeração de entidades | muda fazendas de mobs |

## Como repetir os testes

* `/neostates autoteste` no console do servidor: 53 verificações (50 sem Bukkit), numa área de 9×9 perto do spawn, a
  Y 120, que ele limpa ao terminar.
* `/neostates luz`: fila e totais da migração de luz.
* `/neostates rastro <jogador> <ticks>`: movimento de um jogador, tick a tick, no log do servidor.
* `/neostates sonda`, `ler`, `agua`: estado de uma célula.
