# Plugins de teste

Dois plugins Bukkit mínimos usados nos testes no Crucible ([docs/TESTES.md](../../docs/TESTES.md)). Não fazem parte do
mod nem do build; só vão para uma cópia de teste do servidor.

| Plugin | Para quê |
|---|---|
| `noflow` | faz as vezes de um plugin de proteção: com `/noflow on`, cancela todo `BlockFromToEvent`; `/noflow` mostra quantos viu e cancelou |
| `fishlog` | escreve no console os eventos de pesca do Kunftig (`KunftigFishEvent`) e do Bukkit, com o bloco onde o anzol está |

Para compilar um deles (o `fishlog` também precisa do `kunftig-api` no classpath):

```bash
javac --release 8 -cp <server.jar do Crucible> -d out noflow/NoFlow.java
```

```bash
cp noflow/plugin.yml out/ && (cd out && jar cf ../NoFlow.jar .)
```
