# ADR-0001: onde guardar o waterlogging no 1.7.10

* **Estado**: preliminar. **Revisado na Fase 1**: o dono decidiu não depender do ChunkAPI nem de bibliotecas da
  FalsePattern. A âncora (campo na seção) e o formato (1 bit) ficam; persistência e rede passam a ser nossas. Ver
  [Revisão 1](#revisão-1-sem-chunkapi), que substitui o que este documento diz sobre o ChunkAPI.
* **Contexto de leitura**: [VANILLA-1.7.10.md](../VANILLA-1.7.10.md), [MODERN-1.21.1.md](../MODERN-1.21.1.md),
  [COMPATIBILIDADE.md](../COMPATIBILIDADE.md).

## Decisão

Guardar o waterlogging numa **camada paralela de 1 bit por célula, presa a cada `ExtendedBlockStorage`** por um campo
`@Unique` acrescentado por mixin, e deixar a persistência e a sincronização com o **ChunkAPI** (`SubChunkDataManager`,
`PacketDataManager`, `CubicPacketDataManager`, `BlockPacketDataManager`).

É a Abordagem A do enunciado com uma mudança de âncora: o array fica na seção, não no chunk. Os arrays vanilla da seção
não são tocados (não é a Abordagem C).

Implementação própria, sem depender do FluidLogged, cobrindo as lacunas dele listadas adiante.

> A parte "deixar a persistência e a sincronização com o ChunkAPI" foi **revogada** pela Revisão 1 abaixo.

## Revisão 2: marca de luz

O mesmo composto `NeoStates` do NBT do chunk guarda `Light` (byte): a opacidade da água com que a luz do chunk foi
calculada. Um chunk lido do disco sem a marca é tido como de antes da mudança e tem a luz da água refeita uma vez
(`light/WaterRelight`). Um composto só com `Light` é válido; `Water` passa a ser opcional.

## Revisão 1: sem ChunkAPI

Decisão do dono (Fase 1): o Neo States não declara dependência do ChunkAPI nem do FalsePatternLib.

O que **não** muda: campo `@Unique long[]` em `ExtendedBlockStorage`, 512 bytes por seção com algum bloco
waterlogged, as costuras de leitura e escrita, a fila própria de ticks.

O que passa a ser código nosso:

| Função | Antes (ChunkAPI) | Agora |
|---|---|---|
| Salvar e carregar | `SubChunkDataManager` | `ChunkDataEvent.Save` / `ChunkDataEvent.Load` do Forge: uma lista `NeoStates` no NBT `Level`, um item por seção com bit (`Y` com sinal + 512 bytes) |
| Chunk inteiro para o cliente | `PacketDataManager` | pacote próprio enviado em `ChunkWatchEvent.Watch`, só para chunks que têm algum bit |
| Mudança de célula | `BlockPacketDataManager` | pacote próprio para quem observa o chunk, enviado quando o bit muda |
| Seções de Y < 0 | automático | alcançadas por `BellowAPI.getDeepSection(chunk, sectionY)` ao salvar, carregar e enviar (dependência opcional, por reflexão) |
| Cópia para as threads de meshing do Angelica | automático (`cloneSubChunk`) | um mixin tardio em `ExtendedBlockStorageExt.copyFrom` que copia o campo, ou leitura do mundo vivo por `IBlockAccessExtended.getWorld()` (a decidir na fase de render) |
| Aviso de desinstalação | automático | não há; sem o mod, a lista `NeoStates` do NBT é ignorada e a água hospedada some |

Custos desta escolha, para ficarem registrados:

* **O ChunkAPI continua no pack** (EndlessIDs e BelowCore exigem). A decisão tira a dependência do Neo States, não a
  presença do mod: os pacotes de chunk continuam sendo os que ele reescreve, e o nosso dado viaja ao lado, em pacotes
  nossos.
* **Ordem de chegada no cliente.** O dado da camada chega num pacote separado do `S21` / `S26`. Um chunk reenviado
  inteiro recria as seções no cliente e apaga o campo; o servidor precisa reenviar a camada junto. O Hodgepodge
  reenvia chunks com `new S21PacketChunkData(chunk, true, 0xFFFF)` **sem** disparar `ChunkWatchEvent`: esse caminho
  precisa de um gancho próprio.
* **Y < 0** deixa de ser automático e passa a depender da API do BelowCore e da ordem dos eventos.
* Mais código nosso em salvar e sincronizar, que é onde um erro corrompe ou dessincroniza.

Verificações novas:

* **[A VERIFICAR]** `ChunkDataEvent.Load` dispara depois de o BelowCore recriar as seções profundas (ele carrega pelo
  ChunkAPI dentro de `AnvilChunkLoader`).
* **[A VERIFICAR]** `ChunkDataEvent.Save` / `Load` no Crucible (salvamento assíncrono de chunk).
* **[A VERIFICAR]** todos os caminhos que mandam um chunk inteiro: `ChunkWatchEvent.Watch`, o reenvio do Hodgepodge,
  o `S21` só de seções profundas do BelowCore, troca de dimensão e respawn.
* **[A VERIFICAR]** pacote próprio atravessando Velocity + ProtocolObelisk (canal de `FMLProxyPacket`).

## O problema, reenunciado

O enunciado original era "o metadata tem 4 bits". Três fatos lidos no código mudam o enunciado:

1. No 1.21.1 o estado de fluido **não é armazenado**. `LevelChunkSection` guarda só `BlockState`; o `FluidState` é
   derivado, e waterlogged é uma propriedade booleana cujo fluido é sempre **água fonte**. O dado a guardar é
   **1 bit por célula**.
2. No TEOF o EndlessIDs sobrescreve `ExtendedBlockStorage.getExtBlockMetadata` / `setExtBlockMetadata` e o metadata
   armazenado passa a ter 16 bits. O limite deixou de ser de armazenamento; é de **semântica**: cada bloco é dono do
   próprio metadata e o reescreve inteiro.
3. O mundo tem seções fora de `Chunk.storageArrays`: as de Y < 0 do BelowCore, que são `ExtendedBlockStorage` reais
   salvas e enviadas pelos managers de seção do ChunkAPI.

O requisito então é: 1 bit por célula, que acompanhe **qualquer** `ExtendedBlockStorage` (vanilla ou profunda), sem
passar pelo metadata de ninguém.

## Critérios

| Critério | Peso | Por quê |
|---|---|---|
| Não corromper bloco, metadata nem TileEntity de terceiros | eliminatório | invariante do projeto |
| Funcionar em Y < 0 | eliminatório | BelowCore + VitaGenesis geram água lá |
| Coexistir com EndlessIDs, ChunkAPI, BelowCore, Angelica | eliminatório | já estão no pack |
| Custo de leitura nos laços de física | alto | `handleMaterialAcceleration` roda por entidade por tick |
| Memória | alto | servidor com mundo pré-gerado |
| Leitura segura nas threads de meshing do Angelica | alto | o meshing roda fora da thread principal |
| Desinstalação limpa | médio | o mundo tem de abrir sem o mod |

## As abordagens

### A. Camada paralela (escolhida, com âncora na seção)

**Formato**: `long[64]` por seção (4096 bits = **512 bytes**), `null` enquanto a seção não tem nenhum bloco waterlogged.
Índice `y << 8 | z << 4 | x`, o mesmo da seção.

**Por que na seção e não no chunk**:

| Efeito | Mecanismo lido no código |
|---|---|
| Vale abaixo de Y 0 sem código extra | `DeepDataManager` do BelowCore chama `DataRegistryImpl.writeSubChunkToNBT` / `readSubChunkFromNBT` / `writeToBufferCubic` / `readFromBufferCubic` para cada seção profunda: todo manager de seção registrado vai junto |
| Chega às threads de meshing sem corrida | `ExtendedBlockStorageExt.copyFrom` do Angelica chama `DataRegistry.cloneSubChunk(chunk, storage, this)`; como a cópia é uma subclasse de `ExtendedBlockStorage`, ela tem o mesmo campo `@Unique` |
| Segue o ciclo de vida da seção | seção removida ou recriada leva o dado junto |
| Não compete com o EndlessIDs | ele sobrescreve métodos de bloco e metadata; um campo novo com prefixo próprio não cruza com `b2High`, `b3`, `m1High`, `m2` |

**Persistência** (`SubChunkDataManager`): um `byte[]` de 512 bytes por seção no NBT da seção, mais um byte de versão do
formato. Seção sem bit não escreve nada. O ChunkAPI registra o manager no `level.dat` e avisa se ele sumir
(`uninstallMessage()`).

**Rede**: `PacketDataManager` (seções 0..15 no `S21` / `S26`), `CubicPacketDataManager` (seções profundas),
`BlockPacketDataManager` (`S23` e multi-block). Para `S23` em Y < 0 a seção é resolvida por
`BellowAPI.getDeepSection(chunk, y >> 4)` (dependência opcional, por reflexão ou classe de compatibilidade).

**Custo de leitura**: resolver chunk e seção custa o mesmo que um `getBlock`. Para não pagar isso em toda célula:

1. Rejeição por bloco: um campo booleano em `Block` (acrescentado por mixin, preenchido no `postInit`) diz se o bloco
   **pode** conter água. Ar, cubos opacos e os próprios líquidos saem aqui, com uma leitura de campo.
2. Rejeição por seção: `long[] == null` → sem waterlogging nessa seção.
3. Só então o bit: uma carga de array, um deslocamento, uma máscara.

Nos laços vanilla o bloco da célula já foi lido (`getBlock` é a chamada que a costura envolve), então o passo 1 não
custa uma segunda busca de chunk.

**Memória**: 512 bytes por seção que tem ao menos um bloco waterlogged; zero nas outras. Um chunk com as 16 seções
ocupadas custa 8 KB.

**Extensão futura**: o byte de versão permite trocar o bitset por um array de nibble (tipo de fluido, nível, direção de
bolha) sem migração destrutiva. A API pública fala em "estado de fluido na posição", não em bit
([ADR-0003](ADR-0003-pipeline-de-fluidos.md)).

### B. TileEntity fantasma (rejeitada)

| Problema | Evidência |
|---|---|
| Não cabe onde já há TileEntity | `Chunk.func_150812_a` (`Chunk.java:953`): um mapa por `ChunkPosition`; ao guardar uma nova, **invalida a que estava**. Um baú waterlogged perderia a TileEntity original, que é o caso que o requisito 4.1.2 proíbe |
| Não cabe em bloco sem TileEntity | o mesmo método só guarda se `getBlock(x, y, z).hasTileEntity(metadata)`; laje, escada e cerca devolvem `false`. Forçar exigiria um patch em `Chunk` |
| Leitura lenta | `HashMap` com alocação de `ChunkPosition` por consulta, dentro de `handleMaterialAcceleration` |
| Memória | um objeto, uma entrada de mapa e um `NBTTagCompound` por bloco; ordens de grandeza acima de 1 bit |
| Meshing | o Angelica lê TileEntities por um mapa concorrente com trava de leitura; o custo entra em toda seção |
| Rede | um `S35PacketUpdateTileEntity` por bloco |

### C. Mexer nos arrays da `ExtendedBlockStorage` (rejeitada)

| Problema | Evidência |
|---|---|
| EndlessIDs | `@Overwrite` em `getBlockByExtId`, `func_150818_a`, `getExtBlockMetadata`, `setExtBlockMetadata`; mudar o layout dos mesmos arrays é conflito direto de `@Overwrite` |
| ChunkAPI | a serialização de bloco e metadata é dos managers `BlockIDManager` e `MetadataManager` (e dos do EndlessIDs); um layout novo teria de substituí-los |
| BelowCore | `@Redirect` nos acessos a `storageArrays` em `Chunk` com `require = 5` e `require = 2` |
| Angelica | `ExtendedBlockStorageExt` copia os arrays que conhece |

### C′. Usar os bits altos do metadata de 16 bits do EndlessIDs (rejeitada)

É a variante tentadora, porque o armazenamento já existe. Não serve:

* um bloco que faz `setBlockMetadataWithNotify(x, y, z, novo & 7, …)` apaga o bit (portas, alçapões, escadas ao serem
  colocadas, pistões);
* `getIcon(side, meta)` indexa `IIcon[]` pelo metadata: um valor `>= 16` estoura o array em blocos de mods;
* `damageDropped(meta)`, `getDamageValue`, comparações `meta == n` e `ItemBlock` passam a ver outro valor;
* depende de `extendBlockItem` estar ligado e de o EndlessIDs existir.

## FluidLogged: usar, bifurcar ou escrever

Existe um mod que já faz a Abordagem A: o **FluidLogged** (MEGA / FalsePattern, 0.2.0, LGPL-3.0-only). Ele valida a
arquitetura (campo `@Unique` na seção + ChunkAPI) e o Angelica já tem código de integração com ele.

| Opção | A favor | Contra |
|---|---|---|
| Depender dele | pronto; integração de dados no Angelica | não funciona em Y < 0; 16 KB por seção; o Angelica não desenha o fluido; o tick roda `BlockDynamicLiquid.updateTick` na posição hospedeira (perde tick com o Hodgepodge, e o corpo do método é outro no Crucible); o empurrão passa por `velocityToAddToEntity`, onde o Et Futurum lê o metadata do hospedeiro; suporta qualquer fluido, o que amplia a superfície de teste |
| Bifurcar | parte de código que funciona | as correções tocam armazenamento, rede, tick e render: sobra pouco do original; o fork herda a LGPL |
| **Escrever** | formato de 1 bit; Y < 0; ticks e render próprios; API pensada para o ecossistema | mais trabalho inicial; perde a integração de dados já existente no Angelica |

Proposta: **escrever**, sem copiar código dele (a licença é LGPL e o Neo States ainda não tem licença definida). As
costuras são as mesmas porque são as do vanilla.

## Escrita: quando o bit muda

| Evento | Costura | Regra |
|---|---|---|
| Bloco colocado sobre água fonte | `Chunk.func_150807_a` (`@Inject HEAD`) | se o bloco antigo é água com metadata 0 e o novo aceita água, liga o bit |
| Bloco trocado por outro | idem | se o novo não aceita água, desliga o bit |
| `World.setBlockToAir` numa célula com bit | `World.setBlockToAir` (`@Inject HEAD`) | escreve `Blocks.water` no lugar (regra moderna de `removeBlock`) |
| `setBlock(ar)` direto | cai em `Chunk.func_150807_a` | a água some (regra moderna de `setBlock`) |
| Balde cheio no bloco | `FillBucketEvent` | liga o bit, consome o balde |
| Balde vazio no bloco | `FillBucketEvent` | desliga o bit, entrega o balde de água |
| Água corrente chega ao bloco | leitura no espalhamento | não entra e não destrói o bloco (`canPlaceLiquid` só aceita `Fluids.WATER`, a fonte) |
| Regra das 2 fontes vale para a célula | rotina própria de tick | liga o bit (no 1.21.1 `getNewLiquid` devolve fonte e `placeLiquid` aceita) |

**Invariante da Camada C**: o código do Neo States nunca chama `setBlock`, `setBlockMetadataWithNotify`,
`setTileEntity` nem `removeTileEntity` na célula hospedeira. A única escrita é o bit. Isso é o que garante que o baú, a
placa e qualquer TileEntity de mod fiquem intactos (requisito 4.1.2). O teste de aceitação compara o NBT da TileEntity
antes e depois de encher e esvaziar.

O perigo concreto está na simulação: rodar `BlockDynamicLiquid.updateTick` "como se" a célula fosse água faz o método
escrever na própria célula (`func_149811_n` troca o bloco pelo estático; `setBlockMetadataWithNotify`;
`setBlockToAir`). Por isso a água de um bloco waterlogged espalha por uma rotina **própria**, que só escreve nos
**vizinhos** e reusa as decisões do vanilla por `@Invoker` (`func_149807_p`, `func_149809_q`, busca de declive).

## Leitura: as costuras

Toda consulta vanilla de "há água aqui?" lê o bloco da célula. As costuras envolvem essa leitura (`@WrapOperation` em
`getBlock` e `getBlockMetadata` dentro do método) e respondem "água fonte" quando o bit está ligado:

| Método | Quem depende |
|---|---|
| `World.handleMaterialAcceleration` | `Entity.handleWaterMovement` → `isInWater()`: nado, YesSteveModel, apagar fogo, queda |
| `Entity.isInsideOfMaterial` | respiração, névoa, `isEyeInWater` dos shaders, YesSteveModel sem o Player Backport |
| `World.isAnyLiquid` | `isOffsetPositionInLiquid` (sair pela borda), cavalo do Et Futurum |
| `World.isMaterialInBB` | `handleLavaMovement` e consultas de mods |
| `World.isAABBInMaterial` | anzol (vanilla e Kunftig) |
| `ActiveRenderInfo.getBlockAtEntityViewpoint` | névoa e FOV no cliente |
| `BlockLiquid.getEffectiveFlowDecay`, `func_149804_e` | vetor de fluxo e nível visto pelos vizinhos |
| `BlockLiquid.shouldSideBeRendered`, `isBlockSolid` | faces da água vizinha |
| `RenderBlocks.getLiquidHeight` | altura dos cantos |

No empurrão, a célula waterlogged **não** passa por `block.velocityToAddToEntity` (regra 6 de
[COMPATIBILIDADE.md](../COMPATIBILIDADE.md)): o fluxo dela é somado direto pelo pipeline.

O que as costuras **não** alcançam: código de mod que lê `world.getBlock(x, y, z).getMaterial()` por conta própria.
No ecossistema isso é o `WaterPoseConditions` e o `SwimmingVerticalHooks` do Player Backport, o `kunftig$fluidSurface`
do Kunftig e o barco do Et Futurum. Para os nossos, a saída é a API (`NeoStates.fluidAt(world, x, y, z)`), chamada por
reflexão para não criar dependência.

## Ticks

Um tick agendado de `flowing_water` numa célula que não é água é descartado por `WorldServer.tickUpdates`
(`Block.isEqualTo`). A água de um bloco waterlogged precisa de fila própria:

* fila por mundo no servidor, salva por chunk (`ChunkDataManager`);
* agendada quando o bit liga e quando um vizinho muda (`World.notifyBlockOfNeighborChange`);
* processada depois de `tickUpdates`;
* no Crucible, dispara `BlockFromToEvent` antes de cada espalhamento, para os plugins de proteção.

## Verificações que fecham este ADR

* **[A VERIFICAR]** o mixin de campo `@Unique` em `ExtendedBlockStorage` convive com os dois mixins do EndlessIDs na
  mesma classe (ordem e nomes) no cliente e no Crucible.
* **[A VERIFICAR]** `cloneSubChunk` é chamado para as seções profundas no caminho do Angelica
  (`MixinClonedChunkSection_Deep` devolve a seção; a cópia segue o caminho normal).
* **[A VERIFICAR]** tamanho real do pacote com o manager novo contra o tampão de 32 KB do `DeepDataManager`.
* **[A VERIFICAR]** `S23` em Y < 0: quem chama `writeBlockToPacket` e com qual `y`.
* **[A VERIFICAR]** Velocity + ProtocolObelisk deixam passar o pacote com o bloco a mais.
* **[A VERIFICAR]** `Chunk.func_150807_a` e `World.setBlockToAir` no bytecode do Crucible.
* **[A VERIFICAR]** custo medido: `handleMaterialAcceleration` com e sem a costura, 1000 entidades.
* **[A VERIFICAR]** `BlockPistonBase` do Et Futurum (`@Overwrite`): por onde o bloco movido é escrito e se o bit cai
  como esperado.
* **[A VERIFICAR]** geração de mundo: estruturas que colocam cerca ou laje dentro de água passam a nascer
  waterlogged pela regra de `Chunk.func_150807_a`; decidir se isso vale durante `populate`.
