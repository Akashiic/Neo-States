# Moderno 1.21.1: o comportamento que queremos trazer

Referência do lado moderno, lida no `client.jar` 1.21.1 da Mojang (sha1 `30c73b1c5da787909b2f73340419fdf13b9def88`)
remapeado com os mappings oficiais (`client.txt`) e decompilado com o Vineflower 1.11.1. Nomes em Mojang Mappings.
Os parâmetros não têm nome nos mappings oficiais (o decompilador mostra `$$0`, `$$1`...). As linhas citadas são desse
decompilado e mudam com outro decompilador; o método é a referência estável.

O que não foi lido em código leva a marca **[A VERIFICAR]**.

## 1. Mapa de classes

| Classe (Mojang) | Papel | Equivalente 1.7.10 |
|---|---|---|
| `net.minecraft.world.level.material.Fluid` | tipo de fluido (registro `BuiltInRegistries.FLUID`) | `Material.water` / `Material.lava` + o par de blocos |
| `…material.FluidState` | estado imutável: tipo + `LEVEL` (1..8) + `FALLING` | o metadata do bloco líquido |
| `…material.FlowingFluid` | fluxo, altura, forma, espalhamento | `BlockLiquid` + `BlockDynamicLiquid` |
| `…material.WaterFluid` (`.Source`, `.Flowing`) | parâmetros da água | `BlockLiquid` com `Material.water` |
| `…material.Fluids` | `EMPTY`, `WATER`, `FLOWING_WATER`, `LAVA`, `FLOWING_LAVA` | `Blocks.water` / `flowing_water` / … |
| `net.minecraft.world.level.block.LiquidBlock` | o bloco `water` (um só; `LEVEL` 0..15) | `BlockStaticLiquid` + `BlockDynamicLiquid` |
| `…block.LiquidBlockContainer` | bloco que aceita fluido dentro | não existe |
| `…block.SimpleWaterloggedBlock` | implementação padrão: só `Fluids.WATER` (fonte) | não existe |
| `…block.BucketPickup` | bloco de onde o balde tira fluido | não existe |
| `…block.BubbleColumnBlock` | coluna de bolhas (`DRAG`) | não existe |
| `…block.state.properties.BlockStateProperties#WATERLOGGED` | a propriedade booleana | não existe |
| `net.minecraft.world.entity.Entity#updateFluidHeightAndDoFluidPushing` | detecção + empurrão | `World#handleMaterialAcceleration` + `Entity#handleWaterMovement` |
| `Entity#updateFluidOnEyes` | olhos no fluido | `Entity#isInsideOfMaterial` |
| `LivingEntity#travel(Vec3)` | movimento na água | `EntityLivingBase#moveEntityWithHeading` |
| `net.minecraft.client.Camera#getFluidInCamera` | fluido na câmera | `ActiveRenderInfo#getBlockAtEntityViewpoint` |
| `net.minecraft.client.renderer.FogRenderer` | cor e distância da névoa | `EntityRenderer#updateFogColor` / `setupFog` |
| `net.minecraft.client.renderer.block.LiquidBlockRenderer` | malha do fluido | `RenderBlocks#renderBlockLiquid` |
| `net.minecraft.client.renderer.BiomeColors#getAverageWaterColor` | cor com blend de bioma | `BlockLiquid#colorMultiplier` |
| `net.minecraft.world.level.biome.BiomeSpecialEffects` | `waterColor`, `waterFogColor` por bioma | `BiomeGenBase#waterColorMultiplier` |

## 2. Modelo de dados: o `FluidState` é derivado, não armazenado

* A seção (`LevelChunkSection`) guarda **só** `PalettedContainer<BlockState> states`. Não há contêiner de fluido.
  `LevelChunkSection.getFluidState(x, y, z)` é `this.states.get(x, y, z).getFluidState()`.
* `BlockState.getFluidState()` delega em `Block.getFluidState(state)`. Um bloco waterloggable devolve
  `state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state)` (`SlabBlock`).
* Logo: **waterlogging no 1.21.1 é um bit por bloco, e o fluido é sempre água fonte**. Não existe bloco com água
  corrente dentro. `SimpleWaterloggedBlock.canPlaceLiquid` só aceita `fluid == Fluids.WATER`.
* Níveis: `FluidState.getAmount()` é `8` na fonte e `LEVEL` (1..8) na corrente. Conversão para o metadata antigo:
  `FlowingFluid.getLegacyLevel` = `isSource ? 0 : 8 - min(amount, 8) + (FALLING ? 8 : 0)`. É a mesma codificação do 1.7.10.
* Os ticks de fluido têm fila própria: `ServerLevel.fluidTicks` (`LevelTicks<Fluid>`), separada dos ticks de bloco.
  Um tick de água é agendado para a **posição e o tipo de fluido**, e funciona qualquer que seja o bloco ali.
* 44 classes de `net.minecraft.world.level.block` referenciam `WATERLOGGED`, entre elas `SlabBlock`, `StairBlock`,
  `CrossCollisionBlock` (cercas, painéis), `WallBlock`, `TrapDoorBlock`, `LadderBlock`, `SignBlock`, `ChestBlock`,
  `EnderChestBlock`, `BaseRailBlock`, `LeavesBlock`, `LanternBlock`, `ChainBlock`, `CampfireBlock`, `ScaffoldingBlock`.

## 3. Altura: uma única definição

```java
// FlowingFluid
public float getOwnHeight(FluidState s) { return s.getAmount() / 9.0F; }
public float getHeight(FluidState s, BlockGetter level, BlockPos pos) {
    return hasSameAbove(s, level, pos) ? 1.0F : s.getOwnHeight();
}
```

Fonte `8/9`; corrente `amount/9`; `1` com o mesmo fluido em cima. Render, corpo, olhos e câmera usam todos
`FluidState.getHeight`.

## 4. Vetor de fluxo (`FlowingFluid.getFlow`)

```
para cada direção horizontal d:
    n = fluidState(pos + d)
    se n vazio ou do mesmo tipo (affectsFlow):
        h = n.getOwnHeight()
        se h == 0:
            se !blockState(pos + d).blocksMotion():
                b = fluidState(pos + d abaixo)
                se affectsFlow(b) e b.getOwnHeight() > 0:
                    delta = self.getOwnHeight() - (b.getOwnHeight() - 0.8888889)
        senão: delta = self.getOwnHeight() - h
        v += d * delta
se FALLING:
    se isSolidFace(pos + d, d) ou isSolidFace(pos + d acima, d) para algum d:
        v = normalize(v) + (0, -6, 0)
return normalize(v)
```

`isSolidFace`: mesmo fluido → `false`; `UP` → `true`; `IceBlock` → `false`; senão `state.isFaceSturdy(level, pos, dir)`.

## 5. Entidades na água

### 5.1 Detecção e empurrão (`Entity.updateFluidHeightAndDoFluidPushing(TagKey<Fluid>, double)`)

```
box = getBoundingBox().deflate(0.001)                     # sem o corte de 0.4
para cada célula da caixa:
    f = fluidState(célula)
    se f.is(tag):
        topo = y + f.getHeight(level, pos)
        se topo >= box.minY:
            dentro = true
            altura = max(topo - box.minY, altura)
            se isPushedByFluid():
                fluxo = f.getFlow(level, pos)
                se altura < 0.4: fluxo *= altura
                soma += fluxo; n++
se |soma| > 0:
    se n > 0: soma /= n                                   # média por célula
    se !(this instanceof Player): soma = normalize(soma)  # jogador não é normalizado
    soma *= escala                                        # água 0.014
    se |motion.x| < 0.003 e |motion.z| < 0.003 e |soma| < 0.0045: soma = normalize(soma) * 0.0045
    motion += soma
fluidHeight[tag] = altura
```

* Escala: água `0.014`; lava `0.0023333333333333335`, ou `0.007` em dimensão `ultraWarm`.
* `isInWater()` é `wasTouchingWater`. `isUnderWater()` é `wasEyeInWater && isInWater()`.
* `updateFluidOnEyes`: `y + fluid.getHeight(...) > getEyeY()` na célula dos olhos.
* `getFluidJumpThreshold()`: `0.4`, ou `0` se a altura dos olhos é menor que `0.4`.
* Passageiro de barco fora d'água não conta como na água.

### 5.2 Movimento (`LivingEntity.travel`, ramo da água)

```
arraste = isSprinting() ? 0.9 : getWaterSlowDown()        # 0.8
acel    = 0.02
ef      = atributo WATER_MOVEMENT_EFFICIENCY (metade se fora do chão)
se ef > 0: arraste += (0.54600006 - arraste) * ef ; acel += (getSpeed() - acel) * ef
se DOLPHINS_GRACE: arraste = 0.96
moveRelative(acel, entrada) ; move(...)
se horizontalCollision e onClimbable(): y = 0.2
motion *= (arraste, 0.8, arraste)
motion = getFluidFallingAdjustedMovement(gravidade, estavaCaindo, motion)   # y -= gravidade / 16
se horizontalCollision e isFree(x, y + 0.6 - getY() + yAntes, z): y = 0.3
```

* `getFluidFallingAdjustedMovement`: se há gravidade e não está correndo, `y -= g / 16`. Com `g = 0.08` isso é
  `0.005` por tick. (O ramo `-0.003` só é alcançável com gravidade diferente do padrão: com `g = 0.08` as duas
  condições testam a mesma expressão com sinais opostos.)
* Afundamento terminal: `v = 0.8·v − 0.005` → **−0.025 bloco/tick**.
* Pulo (`LivingEntity.aiStep`): `jumpInLiquid` soma `0.04` quando está na água com `fluidHeight > 0` e (fora do chão ou
  `fluidHeight > limiar`); senão pula do chão. Subida terminal: `u = 0.8·u − 0.005 + 0.04` → **+0.175 bloco/tick**.
* Agachar na água (`LocalPlayer.aiStep`): `goDownInWater` soma `−0.04` → descida terminal **−0.225 bloco/tick**.
* Nado (`Player.travel`): com `isSwimming()`, `y += (olhar.y − y) × (olhar.y < −0.2 ? 0.085 : 0.06)` quando o olhar
  aponta para baixo, o jogador pula, ou há fluido em `y + 0.9`.
* `Entity.updateSwimming`: começa com corrida + `isUnderWater()` + fluido água na célula dos pés; continua com
  corrida + `isInWater()`.

### 5.3 Outras entidades

* `ItemEntity.setUnderwaterMovement`: `x, z *= 0.99`; `y += 0.0005` enquanto `y < 0.06` (o item **sobe**).
* **[A VERIFICAR]** `Boat` (flutuação por `getWaterLevelAbove` e estados `IN_WATER` / `UNDER_WATER`),
  `ExperienceOrb`, `AbstractArrow`, `FishingHook` (lido só pelo Kunftig) e `AbstractFish`.

## 6. Simulação de fluxo (`FlowingFluid.tick` / `spread` / `getNewLiquid`)

* `tick`: se não é fonte, recalcula (`getNewLiquid`); vazio → ar; diferente → `setBlock(flag 2)` + novo tick +
  `updateNeighborsAt`. Depois `spread`.
* `getNewLiquid`: maior `amount` entre os vizinhos horizontais que **passam pela parede** (`canPassThroughWall`, por
  forma de colisão); com 2 ou mais fontes vizinhas e `canConvertToSource` (gamerule `waterSourceConversion`) vira fonte
  se embaixo é sólido ou fonte; com o mesmo fluido em cima vira `getFlowing(8, true)`; senão `maior − dropOff`.
* `spread`: se pode descer, desce; **e se há 3 ou mais fontes vizinhas, também espalha para os lados**. Senão, se é
  fonte ou embaixo não é um buraco (`isWaterHole`), espalha para os lados.
* `getSpread`: o nível escrito no vizinho é o `getNewLiquid` **do vizinho** (não `self − dropOff`); a busca de declive
  usa `getSlopeFindDistance` (água 4; lava 2, ou 4 em `ultraWarm`).
* `canHoldFluid`: `LiquidBlockContainer` → `canPlaceLiquid`; portas, placas (tag), escada de mão, cana, coluna de
  bolhas → não; portais e `structure_void` → não; senão `!blocksMotion()`.
* `spreadTo`: `LiquidBlockContainer` → `placeLiquid` (o bloco fica, ganha água); senão destrói e escreve.
* `WaterFluid`: `getTickDelay = 5`, `getDropOff = 1`, `getSlopeFindDistance = 4`,
  `canBeReplacedWith = (dir == DOWN && !fluid.is(WATER))`.
* Um bloco waterlogged agenda o tick da água em `updateShape` e funciona como fonte para os vizinhos (conta para a
  regra das 2 fontes).

## 7. Waterlogging: as regras

* Colocar: `getStateForPlacement` lê `level.getFluidState(pos).getType() == Fluids.WATER` (só **fonte**).
* Balde cheio (`BucketItem.emptyContents`): se o bloco clicado é `LiquidBlockContainer` e aceita, `placeLiquid`; senão
  tenta a célula adjacente à face.
* Balde vazio: `BucketPickup.pickupBlock` desliga `WATERLOGGED` e devolve o balde de água; se o bloco não sobrevive
  sem água, é destruído.
* Quebrar: `Level.removeBlock` e `Level.destroyBlock` escrevem `getFluidState(pos).createLegacyBlock()`, então a água
  fica. `Level.setBlock(pos, AIR)` direto remove a água junto.
* Pistão: `PistonMovingBlockEntity` desliga `WATERLOGGED` do bloco que chega ao destino.
* Laje dupla não aceita água (`SlabBlock.placeLiquid`).
* O tile entity de um baú waterlogged não muda: o bit está no `BlockState`, o `BlockEntity` é o mesmo objeto.

## 8. Coluna de bolhas (`BubbleColumnBlock`)

* Bloco próprio com `DRAG` (para baixo = magma; para cima = soul sand). `getFluidState` devolve sempre água fonte.
* Só existe sobre `bubble_column`, `magma_block` ou `soul_sand`, e só substitui **água fonte parada**
  (`canExistIn`). `MagmaBlock` / `SoulSandBlock` agendam `tick` em 20 ticks ao serem colocados ou quando a água chega em cima.
* `entityInside`: com ar em cima → `Entity.onAboveBubbleCol(drag)`; senão `Entity.onInsideBubbleColumn(drag)`.

  | Método | Para baixo | Para cima |
  |---|---|---|
  | `onAboveBubbleCol` | `y = max(−0.9, y − 0.03)` | `y = min(1.8, y + 0.1)` |
  | `onInsideBubbleColumn` | `y = max(−0.3, y − 0.03)` + zera a queda | `y = min(0.7, y + 0.06)` + zera a queda |

* Partículas `BUBBLE_COLUMN_UP` / `CURRENT_DOWN` e sons ambiente (1 em 200 por tick de animação).

## 9. Render

* `LiquidBlockRenderer.tesselate`: cor `BiomeColors.getAverageWaterColor` (blend pelo raio da opção de vídeo); face
  oculta por `isFaceOccludedBySelf` (a forma do próprio bloco waterlogged) e por vizinho do mesmo fluido; alturas de
  canto por `calculateAverageHeight`; recuo `0.001`.
* `calculateAverageHeight`: usa os dois lados do canto e só olha a diagonal se algum lado tem altura `> 0`; alturas
  `>= 0.8` pesam 10.
* Faces laterais contra `HalfTransparentBlock` (vidro) ou `LeavesBlock` usam `water_overlay` e não desenham o verso.
* `FluidState.shouldRenderBackwardUpFace`: o topo ganha a face de baixo quando alguma das 9 células ao redor não é o
  mesmo fluido nem bloco sólido.
* Texturas (`assets/minecraft/textures/block/`), medidas no jar:

  | Arquivo | Tamanho | Quadros | Cor média | Alfa |
  |---|---|---|---|---|
  | `water_still.png` | 16×512 | 32 (`frametime 2`) | `#B1B1B1` (**cinza**) | 180 constante |
  | `water_flow.png` | 32×1024 | 32 | `#A9A9A9` | 180 constante |
  | `water_overlay.png` | 16×16 | 1 | `#A5A5A5` | 180 constante |

  A textura é em tons de cinza e a cor vem inteira do bioma.

## 10. Cores por bioma (`data/minecraft/worldgen/biome/*.json`, 64 biomas)

| `water_color` | `water_fog_color` | Biomas |
|---|---|---|
| `#3F76E4` | `#050533` | 50 biomas (o padrão: planícies, florestas, oceano, rio, desertos, Nether, End…) |
| `#3D57D6` | `#050533` | `cold_ocean`, `deep_cold_ocean`, `snowy_beach`, `snowy_taiga` |
| `#3938C9` | `#050533` | `frozen_ocean`, `deep_frozen_ocean`, `frozen_river` |
| `#45ADF2` | `#041633` | `lukewarm_ocean`, `deep_lukewarm_ocean` |
| `#43D5EE` | `#041F33` | `warm_ocean` |
| `#617B64` | `#232317` | `swamp` |
| `#3A7A6A` | `#4D7A60` | `mangrove_swamp` |
| `#0E4ECF` | `#050533` | `meadow` |
| `#5DB7EF` | `#5DB7EF` | `cherry_grove` |

Tag `#minecraft:has_closer_water_fog`: `swamp`, `mangrove_swamp`.

## 11. Névoa submersa (`FogRenderer`)

* `Camera.getFluidInCamera`: `WATER` quando a célula da câmera tem fluido água e `camera.y < y + getHeight(...)`.
* Cor (`setupColor`): `biome.getWaterFogColor()` do bioma na câmera, com interpolação linear de **5000 ms** entre a cor
  anterior e a nova ao trocar de bioma; depois clareia até saturar um canal, proporcional a `getWaterVision()`.
* Distância (`setupFog`): `start = −8`, `end = 96 × max(0.25, getWaterVision())`, `× 0.85` em biomas da tag
  `has_closer_water_fog`; limitada pela distância de render (forma cilíndrica).
* `LocalPlayer.getWaterVision()`: `waterVisionTime` sobe 1 por tick com os olhos na água (até 600) e cai 10 por tick fora;
  `t/100 × 0.6 + (t − 100)/500 × 0.4`.
* Névoa linear; não há mais `GL_EXP` nem dependência do encantamento Respiração.

## 12. A verificar no decompilador

* **[A VERIFICAR]** `Boat`, `ExperienceOrb`, `AbstractArrow`, `FishingHook`, `AbstractFish`, `WaterAnimal`.
* **[A VERIFICAR]** `LavaFluid.getSpreadDelay` (a condição de atraso ×4), fora do escopo da água.
* **[A VERIFICAR]** `ScreenEffectRenderer.renderWater` (a sobreposição submersa) e `GameRenderer.getFov` (o fator de
  FOV submerso usa `options.fovEffectScale`).
* **[A VERIFICAR]** `Attributes.WATER_MOVEMENT_EFFICIENCY` × Depth Strider (a origem do atributo no 1.21.1) e
  `OXYGEN_BONUS`.
* **[A VERIFICAR]** partículas `UNDERWATER`, `BUBBLE`, `SPLASH`, `BUBBLE_POP` e seus equivalentes no 1.7.10 / Et Futurum.
