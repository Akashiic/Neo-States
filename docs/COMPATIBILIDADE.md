# Compatibilidade: quem já mexe na água

Inventário do que cada mod do ecossistema faz no código de fluidos do 1.7.10, lido nas fontes de cada um. Serve para
duas coisas: saber onde um mixin do Neo States colidiria, e saber o que já existe e não deve ser refeito.

Versões lidas: Angelica 2.2.43, Hodgepodge 2.7.189, GTNHLib 0.11.40, Et Futurum Requiem 2.6.59-GTNH, ChunkAPI 0.8.4,
EndlessIDs 1.7.4, FluidLogged 0.2.0 (jars `-sources` dos Mavens da GTNH e da FalsePattern), Crucible (commit `cd1de3d`),
e os repositórios BelowCore, Akashic Player Backport, Kunftig, YesSteveModel, VitaGenesis e AkashicFishing no estado
desta sessão.

**Conferido no TEOF 5.4** (ver [TESTES.md](TESTES.md)): Angelica 2.2.43, Hodgepodge 2.7.219, Et Futurum Requiem
2.6.64-GTNH, GTNHLib 0.11.54, ChunkAPI 0.8.4, EndlessIDs 1.7.4 com `extendBlockItem=true`; sem FluidLogged. Os mixins
lidos aqui são os de Hodgepodge 2.7.189 e Et Futurum 2.6.59: **[A VERIFICAR]** o que mudou até as versões do pack.

Nota antiga: Os documentos dos outros repositórios citam números
diferentes (Angelica 2.2.37 e 2.2.43; Hodgepodge 2.7.189 e 2.7.219; Et Futurum 2.6.59 no servidor e 2.6.64 no cliente;
GTNHLib 0.11.40 e 0.11.51; "TEOF 4.1" e "TEOF 5.3").

## Regras que saem deste inventário

1. **Nunca** injetar em `World.isAnyLiquid` ou `World.checkBlockCollision` de forma que mude a expressão
   `box.minY < 0.0D`. O BelowCore exige as duas ocorrências (`require = 2, allow = 2`); se uma sumir, o jogo não sobe.
2. **Nunca** acrescentar um `4.0D` em `EntityRenderer.setupFog` nem um `STORE` de `double` antes do primeiro em
   `EntityRenderer.updateFogColor`. O BelowCore ancora ali com `require = 1, allow = 1`.
3. **Nunca** acrescentar, remover ou envolver um `LDC 0.800000011920929D` seguido de `DMUL` em
   `EntityLivingBase.moveEntityWithHeading`. O Akashic Player Backport exige exatamente três.
4. **Nunca** usar `@Redirect` em `Chunk.getBlock` dentro de `World.getBlock`: o Hodgepodge já tem um, com `require = 1`.
5. **Nunca** tocar no `0.014D` nem no `Vec3.normalize()` de `World.handleMaterialAcceleration` enquanto o
   `adjustedLiquidPhysics` do Et Futurum estiver ligado.
6. **Nunca** chamar `BlockLiquid.velocityToAddToEntity` numa posição waterlogged: o Et Futurum lê ali o metadata da
   célula como nível de fluido (ver abaixo).
7. Em métodos de `BlockDynamicLiquid`, nada de injeção por ordinal de chamada nem `require` com contagem: o Crucible
   reescreve o corpo.
8. Preferir `@WrapOperation` e `@ModifyExpressionValue` (MixinExtras) a `@Redirect` e `@Overwrite`, para encadear com
   os outros mods.

## BelowCore

| Ponto | O que faz | Consequência |
|---|---|---|
| `MixinWorld_Deep` (`priority = 1100`), `isAnyLiquid` + `checkBlockCollision` | `@ModifyExpressionValue` em `?.minY < 0.0`, devolve `false`; `require = 2, allow = 2` | regra 1. Um `@WrapOperation` em `getBlock` dentro de `isAnyLiquid` é compatível (instrução diferente) |
| `MixinEntityRenderer_DeepFog` | `@ModifyVariable` no primeiro `STORE` de `updateFogColor`; `@ModifyConstant 4.0D` em `setupFog` | regra 2 |
| `MixinWorld_Deep` | `y >= 0` e `y < 0` trocados pelo piso do mundo em `getBlock`, `getBlockMetadata`, `setBlock`, `setBlockMetadataWithNotify`, `getTileEntity`, luz | leituras abaixo de 0 funcionam pela `World` |
| `MixinChunk_Deep` | seções profundas em `ExtendedBlockStorage[] bellow$deep` (32 vagas), **fora** de `Chunk.storageArrays`; `@Redirect` dos acessos a `storageArrays` | quem itera `getBlockStorageArray()` não vê Y < 0 |
| `DeepDataManager` | salva e envia as seções profundas chamando `DataRegistryImpl.writeSubChunkToNBT`, `readSubChunkFromNBT`, `cloneSubChunk`, `writeToBufferCubic`, `readFromBufferCubic` do ChunkAPI | um dado guardado **na `ExtendedBlockStorage`** e registrado como `SubChunkDataManager` + `CubicPacketDataManager` é salvo e sincronizado abaixo de 0 sem código extra |
| `DeepDataManager`, orçamento de pacote | tampão por seção de `max(maxPacketSizeCubic(), 32 KB)`, lido uma vez na construção | se um manager registrado depois estourar isso, as seções profundas **deixam de ser enviadas**. A camada de 1 bit soma ~0,5 KB (o pior caso do EndlessIDs é ~25 KB) |
| `MixinPlayerInstance_Deep` | mudança de bloco em Y < 0 vira `S23PacketBlockChange` ou um `S21` só das seções profundas | o `BlockPacketDataManager` precisa resolver a seção por `BellowAPI.getDeepSection(chunk, y >> 4)` |
| Mixins tardios no Angelica | `ClonedChunkSection.getChunkSection`, `WorldSlice.prepare`, `AngelicaRenderSectionManager`… | precedente de mixin tardio no Angelica dentro do ecossistema |
| Não encontrado | nenhum patch em `handleMaterialAcceleration`, `isMaterialInBB`, `isAABBInMaterial`, `isInsideOfMaterial`, `BlockLiquid*`, ticks agendados | livre |

**[A VERIFICAR]** ticks agendados de fluido abaixo de Y 0: o BelowCore não tem patch para eles e o `WaterProbe` só
cobre sair da água parada.

API pública relevante: `BellowAPI.getMinY(World)`, `getDeepSection(Chunk, sectionY)` (devolve a
`ExtendedBlockStorage` crua), `apiVersion()` = 7. Não há armazenamento por bloco na API.

## Akashic Player Backport

Coremod ASM (`SortingIndex 2001000100`), sem mixins, 22 transformadores.

| Ponto | Impressão digital | Consequência |
|---|---|---|
| `PlayerSwimmingMovementTransformer` → `EntityLivingBase.moveEntityWithHeading(FF)V` | exatamente **3** `LDC 0.800000011920929D` cujo próximo opcode é `DMUL`; envolve o 1º e o 3º | regra 3. Se a contagem mudar, ele registra `Refusing swim movement patch` e o arraste de nado volta a `0.8` |
| `PlayerSwimmingVerticalTransformer` → `EntityPlayerSP.onLivingUpdate` (ou `localOnLivingUpdate` do PlayerAPI) | exatamente 1 `INVOKESPECIAL` para `AbstractClientPlayer.onLivingUpdate` | não injetar um segundo `super.onLivingUpdate` |
| `PlayerEdgeSneakTransformer` → `Entity.moveEntity(DDD)V` | 1 `isSneaking()`, 3 `getOffsetBoundingBox` com `−1.0D` | cuidado se a Camada B tocar em `moveEntity` |
| `PlayerCameraTransformer` → `EntityRenderer.orientCamera(F)V` | `yOffset − 1.62F` único | não tocar |

Como ele decide nado e submersão (roda nos dois lados, em `PlayerTickEvent`):

* `player.isInWater()` (vem de `handleWaterMovement`, então passa pelas costuras do Neo States).
* `WaterPoseConditions.waterAtFeet`: `worldObj.getBlock(x, floor(minY + 0.001), z).getMaterial() == Material.water`.
* `WaterPoseConditions.eyeSubmerged`: mesmo teste de material na célula dos olhos + `1 − getLiquidHeightPercent(meta)`.
* `SwimmingVerticalHooks`: `getBlock(floor(posY + 0.9)).getMaterial().isLiquid()`.

**Estes três leem o bloco direto.** Num bloco waterlogged o material é o da laje, e o nado nunca começa ali. Não há
costura no vanilla que resolva isso: é preciso que o Player Backport consulte a API do Neo States (por reflexão, como
o YesSteveModel faz com ele). É uma mudança pequena **no outro repositório**, a combinar.

`isInsideOfMaterial` não é usado pelo Player Backport.

## Kunftig

| Ponto | O que faz | Consequência |
|---|---|---|
| `EntityFishHookMixin` | ancora na **única** chamada de `World.isAABBInMaterial` em `EntityFishHook.onUpdate` (`@ModifyArg`, `@Redirect` do primeiro `isRemote` depois dela, `@Inject` em `setPosition`) | não acrescentar outra chamada de `isAABBInMaterial` nesse método. Envolver `getBlock` **dentro** de `World.isAABBInMaterial` é compatível |
| `kunftig$fluidSurface` | `getBlock(x, y, z).getMaterial()` + `ModernBobber.surfaceHeight(metadata, fluidoEmCima)` | lê bloco e metadata direto; anzol em bloco waterlogged vê "fora d'água" |
| `ModernBobber.surfaceHeight` | `1` com fluido em cima, senão `1 − (level + 1)/9` | **igual** à altura do Neo States (delta A-1). Não muda enquanto a altura lógica não mudar |
| `HookWorld.fluidFraction` / `OpenWater` | `isAABBInMaterial` em 5 fatias; água aberta exige `getMaterial() == fluid && metadata == 0` | célula waterlogged conta como inválida para água aberta (aceitável; é o que o 1.21.1 faz com blocos não-água) |
| `FlightClient.touchesLiquidOrWeb` | varre `getMaterial().isLiquid()` | não vê waterlogged |
| mixins sem `defaultRequire` | uma injeção perdida não derruba o jogo | regressão silenciosa: precisa de telemetria |

Invariante da pesca: a altura que o anzol usa é `8/9` na fonte. A Fase 0 não propõe mudar a altura lógica; qualquer
deformação futura (ondas) passa a valer para o anzol só se o Kunftig migrar para o provider do Neo States
([ADR-0003](adr/ADR-0003-pipeline-de-fluidos.md)). Telemetria proposta: registrar, por anzol, a superfície do
`ModernBobber` e a do provider, e acusar diferença maior que `1e-6`.

## YesSteveModel (3D Futura Engine)

Só cliente. Nenhum mixin em código de fluido.

* `inWater = p.isInWater()`, `inWaterOrRain = p.isWet()`.
* Com o Player Backport presente (`PoseBridge`, por reflexão): `swimming`/`crawling` vêm da pose dele.
* Sem ele: `SwimState.update(isSprinting, isInWater, isInsideOfMaterial(Material.water), …)`.
* Molang: `query.is_in_water`, `is_in_water_or_rain`, `is_swimming`. Não há `is_underwater`.

Tudo passa por `isInWater()` e `isInsideOfMaterial()`, que são costuras do Neo States. Não precisa de mudança no
YesSteveModel; a dessincronia possível é só a do Player Backport descrita acima.

## VitaGenesis

* Escreve `Blocks.water` / `Blocks.lava` com metadata 0 direto nas seções profundas; não usa os blocos dinâmicos.
* `FluidActivation` acorda fontes com `onNeighborBlockChange` e testa `b == Blocks.water` + `getExtBlockMetadata == 0`.
* `Dripstone`: "Et Futurum has no waterlogging, so pointed dripstone is only placed in air". Com a Camada C isso pode
  mudar, pela API.

## AkashicFishing

Plugin Bukkit. Lê lava e água só pelo Kunftig (`hook.isInLava()`) e por `Material.WATER` / `STATIONARY_WATER` do Bukkit
em dois pontos (queimar a linha, dissolver o totem). Não usa altura de fluido. Bloco waterlogged aparece para o Bukkit
como o bloco hospedeiro.

## Angelica

| Ponto | O que faz | Consequência |
|---|---|---|
| Meshing (`AngelicaChunkBuilderMeshingTask`) | para cada bloco não-ar e cada passe: `block.canRenderInPass(pass)` → `RenderBlocks.renderBlockByRenderType` num `Tessellator` de captura | o **contrato vanilla** de render por bloco continua valendo: `renderBlockLiquid` é o método vanilla |
| `WorldSlice` / `ClonedChunkSection` | cópia das seções para as threads de meshing; com o ChunkAPI presente a cópia é `DataRegistry.cloneSubChunk(chunk, storage, this)` | um `SubChunkDataManager` do Neo States é copiado para a thread de meshing sem código extra |
| `WorldSlice implements FLBlockAccess`, `ClonedChunkSection.getFluid` | integração nativa com o mod **FluidLogged** (`ModStatus.isFluidLoggedLoaded`) | o Angelica expõe o dado, mas **não desenha** o fluido de um bloco fluidlogged: nada no laço de meshing consulta `fl$getFluid` **[A VERIFICAR em jogo]** |
| `MixinBiomeGenBase` (`priority = 1100`) | reaproveita o objeto `BiomeEvent.GetWaterColor` por thread | o evento continua sendo disparado, inclusive nas threads de meshing: o handler tem de ser thread-safe |
| `celeritas.biome_blending.MixinBlockLiquid` / `MixinRenderBlocks` | `@Surround` em `BlockLiquid.colorMultiplier` e em `renderBlockLiquid`: blend suave por vértice (`SmoothBiomeColorCache.ColorType.WATER`) | o blend usa a cor que sai do evento; só vale para `Blocks.water` e `Blocks.flowing_water` |
| `mcpatcherforge.cc.block.MixinBlockLiquid` | `@Inject HEAD` cancelável em `colorMultiplier` / `getBlockColor` (Custom Colors) | um resource pack com `colormap` de água ganha da nossa cor |
| `mcpatcherforge.cc…MixinEntityRenderer` | `@Inject` em `updateFogColor` depois dos acessos de `ordinal` 11 e 12 ao campo `fogColorBlue` | não acrescentar leituras nem escritas de `fogColorBlue` antes desses pontos |
| `rendering.MixinBlockLiquid` + `BlockLiquidFlowInvoker` | `@Redirect` da chamada a `getFlowVector` em `getFlowDirection`; `@Invoker("getFlowVector")` | já existe um invoker para o método privado |
| `Iris.setShaderMaterialOverride(Block, int)` / `resetShaderMaterialOverride()` | marca os vértices seguintes com o id de shader de outro bloco | permite desenhar a água de um bloco waterlogged como `minecraft:water` para o shaderpack |
| `CommonUniforms.isEyeInWater` | `client.thePlayer.isInsideOfMaterial(Material.water)` | o uniforme dos shaders segue a costura de `isInsideOfMaterial` |
| `resolvePassMaterial` | lava → sólido; o resto pelo passe (passe 1 → translúcido) | geometria desenhada no passe 1 sai translúcida, seja qual for o bloco |

O plano de interceptação está em [ADR-0002](adr/ADR-0002-interceptacao-visual.md).

## FluidLogged (MEGA / FalsePattern)

Mod 1.7.10 de "waterlogging com qualquer fluido", `modid fluidlogged`, versão 0.2.0 (julho de 2025), LGPL-3.0-only,
depende de ChunkAPI ≥ 0.6.4 e FalsePatternLib. Não está no TEOF, até onde os documentos mostram **[A VERIFICAR]**.

Como funciona:

* Armazenamento: campo `@Unique Fluid[] fl$fluidLog` (4096 referências) em `ExtendedBlockStorage`, salvo e enviado por
  um `DataManager` do ChunkAPI (`PacketDataManager`, `ChunkDataManager`, `SubChunkDataManager`,
  `BlockPacketDataManager`).
* Escrita: `@Inject HEAD` em `Chunk.func_150807_a` recalcula se o novo bloco pode ficar com o fluido;
  `World.setBlockToAir` devolve o bloco do fluido.
* Leitura: `@WrapOperation` de `World.getBlock` dentro de `handleMaterialAcceleration`, `isAnyLiquid`, `isMaterialInBB`,
  `Entity.isInsideOfMaterial`, `ActiveRenderInfo.getBlockAtEntityViewpoint`; `@Redirect` em
  `BlockLiquid.func_149804_e`, `getEffectiveFlowDecay`, `func_149805_n`, `shouldSideBeRendered`,
  `RenderBlocks.getLiquidHeight`.
* Ticks: fila própria em `WorldServer` (`@Inject RETURN` em `tickUpdates`), salva no NBT do chunk.
* Simulação: chama `BlockDynamicLiquid.updateTick` na posição hospedeira, com um guarda para não virar bloco estático.
* Render vanilla: ASM em `WorldRenderer.updateRenderer` com índices de variável local fixos (`ALOAD 16`, `ILOAD 23`…).
* Lista padrão: `BlockChest`, `BlockEnderChest`, `BlockFence`, `BlockPane`, `BlockLadder`, `BlockRailBase`,
  `BlockSign`, `BlockSlab`, `BlockStairs`, `BlockTrapDoor`.

Lacunas para o nosso caso:

| Lacuna | Evidência |
|---|---|
| Não funciona em Y < 0 | `ChunkMixin.fl$getFluid`: `if (Y < 0) return null`; não implementa `CubicPacketDataManager`, que é o caminho que o BelowCore usa |
| Memória | `Fluid[4096]` por seção com qualquer bloco fluidlogged: 16 KB (ponteiros comprimidos) contra 512 bytes de um bitset |
| Não desenha sob o Angelica | o gancho de desenho só existe em `WorldRenderer.updateRenderer` |
| Conflito com o Et Futurum | devolve o bloco do fluido em `handleMaterialAcceleration` e deixa o vanilla chamar `velocityToAddToEntity`, onde o Et Futurum lê o metadata do hospedeiro (regra 6) |
| Tick perdido com o Hodgepodge | o `@Inject HEAD` do Hodgepodge em `BlockDynamicLiquid.updateTick` reagenda com `scheduleBlockUpdate(x, y, z, this, …)`; numa posição hospedeira esse tick de bloco é descartado |
| Leituras diretas | `isAABBInMaterial` e os testes de material do Player Backport e do Kunftig não são cobertos |

A decisão sobre reaproveitar, bifurcar ou escrever do zero está em
[ADR-0001](adr/ADR-0001-armazenamento-waterlogging.md).

## ChunkAPI

Assume a serialização de chunk: `AnvilChunkLoaderMixin`, `S21PacketChunkDataMixin`, `S22…`, `S23…`,
`S26PacketMapChunkBulkMixin` (há uma variante para Thermos), `PlayerInstanceMixin`. Já está no TEOF (o EndlessIDs e o
BelowCore dependem dele).

Interfaces de `com.falsepattern.chunk.api.DataManager`:

| Interface | Para quê |
|---|---|
| `PacketDataManager` | dado no `S21` / `S26` (seções 0..15) |
| `CubicPacketDataManager` (experimental) | dado por `ExtendedBlockStorage` avulsa; é o que o BelowCore usa abaixo de 0 |
| `BlockPacketDataManager` | dado no `S23` (e no multi-block) |
| `ChunkDataManager` | NBT por chunk |
| `SubChunkDataManager` | NBT por seção, e `cloneSubChunk` (usado pelo Angelica) |

O `StorageDataManager` também guarda no `level.dat` a lista de managers e avisa quando um some
(`uninstallMessage()`), o que dá a migração e a desinstalação de graça.

Licença: LGPL-3.0-only com permissão adicional para as classes de API.

## EndlessIDs

`@Overwrite` em `ExtendedBlockStorage.getBlockByExtId`, `func_150818_a`, `getExtBlockMetadata`,
`setExtBlockMetadata`. Acrescenta `b2High`, `b3`, `m1High`, `m2`: com `extendBlockItem` ligado, **o metadata
armazenado tem 16 bits**.

Isso muda o enunciado do "limite de 4 bits": no TEOF o armazenamento não é mais o limite. O que continua limitado é a
**semântica**: cada bloco interpreta o próprio metadata (`meta & 7`, `meta == 2`, índice de `IIcon[]`,
`damageDropped(meta)`) e reescreve o valor inteiro em `setBlockMetadataWithNotify`. Um bit alto reservado seria apagado
pelo próprio bloco ou estouraria um array. Por isso o metadata estendido **não** serve para o waterlogging (variante
C′ do ADR-0001). **[A VERIFICAR]** se `extendBlockItem` está ligado no TEOF.

## Hodgepodge

| Ponto | Padrão | Consequência |
|---|---|---|
| `MixinBlockDynamicLiquid`: `@Inject HEAD` cancelável em `updateTick`; sem chunks num raio de 6, reagenda com `tickRate × 5` | ligado | não rodar `updateTick` vanilla numa posição hospedeira (o reagendamento é um tick de bloco e se perde) |
| `MixinBlockStaticLiquid`: `@Inject` em `updateTick` (lava) | ligado | não afeta a água |
| `MixinWorldGetBlock`: `@Redirect` de `Chunk.getBlock` em `World.getBlock`, `require = 1` | ligado | regra 4 |
| `MixinEntityLivingBase_GravityFix`: `@Inject HEAD` cancelável em `moveEntityWithHeading` | ligado, cliente | no cliente, entidades vivas que não são jogadores **pulam o método inteiro** |
| `MixinWorldServer_PendingTickIndex`: `@Overwrite getPendingBlockUpdates` | ligado; **desligado sob Bukkit** | não vale no Crucible |
| `MixinWorldServer_LimitUpdateRecursion` | 256 | corta cascatas com `scheduledUpdatesAreImmediate` |
| `MixinItemGlassBottle` | ligado | só `Blocks.water` enche garrafa; bloco waterlogged não enche |
| `MixinBlock_LighterWater` | desligado | opacidade da água 3 → 1 ("to match modern"), já existe como opção |
| `MixinChunk_SendWithoutPopulation` + reenvio `new S21PacketChunkData(chunk, true, 0xFFFF)` | ligado | passa pelo mixin do ChunkAPI; nada a fazer |
| `MixinPacketBuffer`: `@Overwrite` de leitura e escrita de NBT, GZIP em pool | ligado | não mandar o dado da camada por NBT em `PacketBuffer` (o ChunkAPI usa `ByteBuffer`) |
| `MixinBlockFence`: `@Overwrite canConnectFenceTo` | ligado | o Et Futurum sobrescreve o mesmo método com `priority = 0` |

Nenhum mixin em `handleWaterMovement`, `isInWater`, `isInsideOfMaterial`, `handleMaterialAcceleration`, `isAnyLiquid`,
`isMaterialInBB`, `isAABBInMaterial`, `setupFog`, `getBlockAtEntityViewpoint`, `renderBlockLiquid`,
`ExtendedBlockStorage`, pacotes `S21`/`S22`/`S23`/`S26`, `ItemBucket`, `BlockSlab`, `BlockStairs`.

No Crucible, toda entrada com `addExcludedMod(TargetedMod.BUKKIT)` fica desligada no servidor.

A opção `useLighterWater` do Hodgepodge faz o mesmo que `modernWaterLight` (opacidade 1). Com qualquer uma das duas
ligada, a migração de luz dos chunks antigos (`relightOldChunks`) vale.

## Et Futurum Requiem

O mod que mais sobrepõe o escopo do Neo States.

| Recurso | Opção (padrão) | O que faz | Consequência |
|---|---|---|---|
| Empurrão de líquido | `adjustedLiquidPhysics` (ligado) | `World.handleMaterialAcceleration`: `@ModifyConstant 0.014D` (itens × 4) e `@WrapOperation` em `Vec3.normalize` (mantém a magnitude até 1.15 em x e z); `BlockLiquid.velocityToAddToEntity`: `@ModifyVariable` que escala por `getBlockMetadata(x, y, z)` (0.4 a 1.0); `Entity.onEntityUpdate`: lava empurra | regras 5 e 6. Deltas C-4 a C-6 e C-8 já têm dono |
| Itens flutuam | `liquidItemFloat` (ligado) | `EntityItem.onUpdate`, `priority = 1002`; detecta por `getMaterial().isLiquid()` + `getLiquidHeightPercent(meta)` | delta D-11 já tem dono; item em bloco waterlogged não flutua |
| Coluna de bolhas | `enableBubbleColumns` (ligado) | `BlockBubbleColumn extends BaseBlock` (não é `BlockLiquid`), `Material.water`, metadata 0 (sobe) ou 8 (desce); desenha com `renderer.renderBlockLiquid(Blocks.water, x, y, z)`; nasce por um `IWorldAccess` | não reimplementar. As costuras de render recebem `Blocks.water` numa posição cujo bloco não é `BlockLiquid` |
| Barcos novos | `enableNewBoats` (ligado) | `EntityNewBoat` lê `getMaterial() == Material.water` + metadata | barco não vê waterlogged |
| Esponja, pó de concreto, Frost Walker | ligados | testes por `Material.water` ou por `Blocks.water` / `flowing_water` | não veem waterlogged |
| Textura de água em cinza | só com o caldeirão de poção | `GrayscaleWaterResourcePack`: um `IResourcePack` que converte `water_still` em cinza **em tempo de execução** | a técnica resolve a textura moderna sem redistribuir arquivos da Mojang |
| Espectador | `enableSpectatorMode` (ligado) | sobrescreve `handleWaterMovement()` em `EntityPlayer`; cancela `EntityViewRenderEvent.FogDensity` em líquido | nosso handler de `FogDensity` precisa respeitar evento já cancelado |
| Elytra | `enableElytra` (ligado) | `@Inject HEAD` cancelável em `moveEntityWithHeading` | nenhuma |
| `isLadderFix` | ligado | `@ModifyExpressionValue` em `isCollidedHorizontally` (`ordinal = 2`) em `moveEntityWithHeading` | não acrescentar leituras de `isCollidedHorizontally` antes dessa |
| Observador | ligado | `@Inject` em `World.markAndNotifyBlock` e `setBlockMetadataWithNotify` | nenhuma |
| Pistões | `betterPistons` (ligado) | `@Overwrite` de `BlockPistonBase.updatePistonState` e `onBlockEventReceived` | o delta F-6 depende de como este código move blocos **[A VERIFICAR]** |

Não existe no Et Futurum: nado, kelp, alga, coral, waterlogging (só uma interface vazia `IFakeWaterloggingRenderer`),
cor de água por bioma, névoa submersa.

Blocos dele que são waterloggable no 1.21.1: `BaseSlab`, `BaseStairs`, `BaseWall`, `BaseTrapdoor`,
`BlockIronTrapdoor`, `BlockCopperTrapdoor`, `BlockWoodFence`, `BlockModernWoodFence`, `BlockWoodFenceGate`,
`BlockChain`, `BlockLightningRod`, `BlockMangroveRoots`, `BlockBarrier`, `BlockLight`, `BlockLantern`,
`BlockPointedDripstone`, `BlockAmethystCluster`, `BlockGlowLichen`, `BlockWoodSign`, `BlockCopperGrate`.

## GTNHLib

Não toca em fluido. Dois pontos de render valem para o nosso código:

* `MixinRenderBlocks`: `@Inject` em `renderBlockByRenderType` depois de `setRenderBoundsFromBlock` (blocos com modelo).
* ASM `TessellatorRedirector`: troca todo `GETSTATIC Tessellator.instance` por `TessellatorManager.get()`, **inclusive
  nas classes do Neo States**. Roda depois dos mixins.

Infraestrutura aproveitável: `@Config` com `@Config.Sync` (servidor → cliente), `NetworkChannel` / `IPacket`,
`BlockPos`, `CoordinatePacker`, `GameRuleRegistry` (para um `waterSourceConversion`), `BlockPropertyRegistry` (dá para
registrar uma propriedade `waterlogged` para depuração e comandos), `@EventBusSubscriber`. Os construtores de mixin
(`IMixins`, `MixinBuilder`, `ITargetMod`) agora vêm de `com.gtnewhorizon.gtnhmixins.builders` (GTNHMixins / UniMixins).

## Crucible

O servidor reescreve o código-fonte das classes de fluido (patches do CraftBukkit, Cauldron, Thermos e PaperSpigot):

| Classe | Mudança | Consequência |
|---|---|---|
| `BlockDynamicLiquid.updateTick` | dispara `BlockFromToEvent` para baixo e para cada lado; as 4 chamadas laterais de `func_149813_h` viram **1 dentro de um laço**; `notifyBlocksOfNeighborChange` vira 5 `notifyBlockOfNeighborChange`; fonte infinita atrás de `cauldron_settings_infiniteWaterSource`; guarda `getType(...)` do PaperSpigot | regra 7. O bytecode do cliente e o do servidor **não são o mesmo** |
| `BlockLiquid.func_149805_n` | `l <= 4` → `l > 0` (PaperSpigot) | lava × água difere do cliente |
| `BlockStaticLiquid.updateTick` | `BlockIgniteEvent` | nenhuma |
| `WorldServer` | listas de tick com `LinkedHashSet` sincronizado; limite por tick diferente; aviso em vez de exceção quando dessincronizam | uma fila própria de ticks evita depender dessas listas |
| `Entity.inWater` | `protected` → `public` | nenhuma |

Não encontrado nos patches: `handleMaterialAcceleration`, `isAnyLiquid`, `isMaterialInBB`, `isAABBInMaterial`,
`isInsideOfMaterial`, `handleWaterMovement`.

`BlockFromToEvent` é o que plugins de proteção (WorldGuard e parecidos) usam para barrar fluxo de líquido entre
regiões. Se a água de um bloco waterlogged espalhar por código nosso, **o evento tem de ser disparado por nós** no
Crucible, ou a proteção fura.

**[A VERIFICAR]** com `javap` no `server.jar` real: o bytecode final de `BlockDynamicLiquid`, `World`, `WorldServer`,
`Chunk` e `Entity`, e os patches de `Chunk.func_150807_a` e `World.setBlock`.

O `PlayerContainerFix` do TEOF deixa `org.bukkit.World` com tipos ofuscados (nota do AkashicFishing): nada de
`getMethod` em classes Bukkit.

### Luz

O `World.updateLightByType` do Crucible pode rodar numa thread própria (`use-async-lighting` do `spigot.yml`, herdado
do PaperSpigot). A migração de luz do Neo States não passa por ele: escreve os arrays de luz das seções na thread do
mundo e manda a luz nova aos clientes num pacote próprio (`MessageLight`), sem reenviar o chunk.

## Velocity + ProtocolObelisk

O servidor fica atrás de um proxy que decodifica pacotes. O ChunkAPI muda o formato de `S21`, `S22`, `S23` e `S26`, e
isso já atravessa o proxy hoje (EndlessIDs). A camada do Neo States viaja dentro desses mesmos pacotes.
**[A VERIFICAR]** em servidor de teste.

## Outros

* **PlayerAPI 1.5.0**: reescreve `EntityPlayerSP` e `EntityPlayerMP` (`onLivingUpdate` → `localOnLivingUpdate`).
  Qualquer gancho em `onLivingUpdate` do jogador precisa mirar o corpo `local*`.
* **DistantHorizons**: o carregador aparece na lista de coremods do pack. **[A VERIFICAR]** de onde os LODs tiram a cor
  da água.
* **Biomes O' Plenty**: o Angelica, o Hodgepodge e o GTNHLib têm mixins tardios no `FogHandler` dele.
  **[A VERIFICAR]** se está no TEOF e como o handler de névoa dele convive com o nosso.
* **Fluidos do Forge** (`BlockFluidClassic`, `BlockFluidFinite`): fora do escopo da água vanilla; ficam como estão.
