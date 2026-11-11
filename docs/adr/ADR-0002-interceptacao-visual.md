# ADR-0002: onde interceptar o visual (vanilla e Angelica)

* **Estado**: preliminar (Fase 0).
* **Contexto de leitura**: [COMPATIBILIDADE.md](../COMPATIBILIDADE.md#angelica),
  [DELTAS.md](../DELTAS.md#g-visual).

## Decisão

Interceptar no **contrato vanilla que os dois pipelines honram**, nunca nas classes internas do Angelica:

| Efeito | Ponto | Tipo |
|---|---|---|
| Cor da água por bioma | `BiomeEvent.GetWaterColor` | evento do Forge, sem mixin |
| Textura em cinza | sprite gerado em tempo de execução no `TextureStitchEvent.Pre` | evento do Forge, sem mixin |
| Cor da névoa submersa | `EntityViewRenderEvent.FogColors` | evento do Forge, sem mixin |
| Distância da névoa submersa | `EntityViewRenderEvent.FogDensity` (cancelado, com `glFog*` próprio) | evento do Forge, sem mixin |
| "A câmera está na água?" em bloco waterlogged | `ActiveRenderInfo.getBlockAtEntityViewpoint` | `@WrapOperation` em `getBlock` |
| "Os olhos estão na água?" | `Entity.isInsideOfMaterial` | `@WrapOperation` em `getBlock` (a mesma costura da física) |
| Desenhar a água de um bloco waterlogged | `Block.canRenderInPass` + `RenderBlocks.renderBlockByRenderType` | mixins em classes vanilla |
| Dado da camada na thread de meshing | `WorldSlice` do Angelica | **um** mixin tardio de acesso, opcional e com queda segura |

Nenhum `@Overwrite`. Nenhuma injeção em `EntityRenderer.setupFog` ou `updateFogColor`, que já têm dono (BelowCore,
Custom Colors do Angelica, Hodgepodge).

## Por que o contrato vanilla basta

O Angelica 2.2.43 (Celeritas) **não reimplementa** o desenho do líquido. O laço de `AngelicaChunkBuilderMeshingTask`
faz, para cada bloco não-ar e cada passe:

```
canRender = block.canRenderInPass(pass)            // ou a regra do shaderpack
drawBlock: setRenderPass(pass); block.canRenderInPass(pass);
           tessellator.startDrawingQuads();
           renderBlocks.renderBlockByRenderType(block, x, y, z);   // método vanilla
material = pelo passe (passe 1 → translúcido; lava → sólido)
```

`renderBlockByRenderType` despacha para `renderBlockLiquid`, o método vanilla (com `@Surround` do Angelica para o
blend). O `IBlockAccess` é o `WorldSlice`. `ForgeHooksClient.getWorldRenderPass()` é seguro entre threads (o Angelica o
redireciona para um `ThreadLocal`).

Consequência: o que for feito em `BlockLiquid.colorMultiplier`, `BiomeGenBase.getWaterColorMultiplier`,
`RenderBlocks.getLiquidHeight` e `renderBlockLiquid` vale nos dois pipelines, com e sem shader.

## 1. Cor da água

`BiomeGenBase.getWaterColorMultiplier()` dispara `BiomeEvent.GetWaterColor` em `MinecraftForge.EVENT_BUS`. O Angelica
mantém o evento (só reaproveita o objeto por thread). O handler do Neo States define `event.newColor`.

Regras do handler:

* **Thread-safe e sem estado**: roda nas threads de meshing. Função pura do bioma (tabela pré-calculada por id).
* **Conversão de espaço de cor**. No 1.7.10 a textura já é azul e o multiplicador é branco; no 1.21.1 a textura é
  cinza e a cor vem inteira do bioma. Com a textura em cinza ativa, `newColor` é a cor moderna do bioma. Biomas de
  mods que definem `waterColorMultiplier` próprio (pensado para a textura azul) são convertidos:
  `cor = corBaseModerna × multiplicador` por canal. **[A VERIFICAR]** contra biomas reais do pack.
* **Mapa de biomas**: os 64 biomas do 1.21.1 não existem no 1.7.10. Tabela por tipo (`BiomeDictionary.Type`) e por id,
  em arquivo de configuração: `OCEAN` + `COLD` → `#3D57D6`, `SWAMP` → `#617B64`, padrão `#3F76E4`, e assim por diante.
* **Custom Colors**: um resource pack com colormap de água ganha (o mixin do MCPatcher cancela `colorMultiplier` antes).
  É o comportamento esperado.

O blend entre biomas fica como está: 3×3 no vanilla; suave por vértice no Angelica (ele já trata `Blocks.water` e
`Blocks.flowing_water` como `ColorType.WATER`).

## 2. Textura

Medido nos jars: a textura do 1.7.10 é azul (`#2F43F4`) com alfa variando de 168 a 226; a do 1.21.1 é cinza
(`#B1B1B1`) com alfa 180 constante. Sem trocar a textura, a cor por bioma multiplica azul por azul.

**Os arquivos da Mojang não podem ir no jar do mod.** Duas saídas, não excludentes:

1. **Converter em tempo de execução** (proposta): no `TextureStitchEvent.Pre`, registrar sprites próprios que leem
   `water_still` e `water_flow` do resource pack ativo e geram a versão em cinza, normalizada para a luminância média
   da moderna, com alfa 180. Funciona com qualquer resource pack. O Et Futurum já usa a mesma técnica para o caldeirão
   de poção (`GrayscaleWaterResourcePack`), o que mostra que ela convive com o pack.
2. Um resource pack opcional do modpack com arte própria.

`BlockLiquid.getIcon` passa a devolver os sprites em cinza para `Material.water` (um `@Inject` cancelável em
`getIcon(II)`, ou a troca do array privado `field_149806_a` depois do stitch). **[A VERIFICAR]** a interação com o
`BlockIconTransformer` do GTNHLib, que insere um retorno antecipado em todo `getIcon`.

Quem mais desenha com o ícone da água: o caldeirão, itens de balde em tanques de mods, a coluna de bolhas do Et
Futurum. Com a textura em cinza eles precisam da cor. **[A VERIFICAR]** lista de quem usa `Blocks.water.getIcon`.

## 3. Névoa submersa

`ActiveRenderInfo.getBlockAtEntityViewpoint` decide tudo no 1.7.10 (cor, densidade, FOV). Os dois eventos do Forge já
entregam `event.block`, que é o retorno desse método.

**Cor** (`EntityViewRenderEvent.FogColors`, disparado no fim de `updateFogColor`):

* se `event.block.getMaterial() == Material.water`: cor de névoa do bioma na câmera, com a transição de 5 s entre
  biomas e o clareamento por `waterVision` do 1.21.1;
* o Angelica (Custom Colors) e o BelowCore escrevem antes do evento, então o nosso valor é o final. Para respeitar um
  resource pack com cor de névoa própria, o handler cede quando Custom Colors está ativo com colormap submerso.
  **[A VERIFICAR]** como detectar isso (`ColorizeWorld.computeUnderwaterColor`).

**Distância** (`EntityViewRenderEvent.FogDensity`, disparado no começo de `setupFog`):

* cancelar o evento faz o vanilla executar só `glFogf(GL_FOG_DENSITY, event.density)`. O handler configura antes
  `GL_FOG_MODE = GL_LINEAR`, `GL_FOG_START = −8`, `GL_FOG_END = 96 × max(0.25, waterVision)` (× 0.85 em pântanos,
  limitado pela distância de render) e cancela;
* se o evento **já chega cancelado** (o modo espectador do Et Futurum cancela em líquido), não fazer nada;
* prioridade `LOWEST`, para que mods com névoa própria decidam antes.

`waterVision` é um contador no cliente (sobe 1 por tick com os olhos na água até 600, cai 10 por tick fora), atualizado
em `ClientTickEvent`.

Com shaderpack ativo, a névoa é do shader; ele lê `isEyeInWater` e a cor de névoa do estado GL. **[A VERIFICAR]** se o
Iris do Angelica lê `GL_FOG_START` / `GL_FOG_END` do estado ou assume `GL_EXP`.

## 4. Desenhar a água de um bloco waterlogged

É o único ponto em que o Angelica obriga a uma escolha.

**Problema.** Um bloco waterlogged (uma laje) responde `canRenderInPass(1) == false`, então ninguém desenha nada no
passe translúcido naquela célula. O FluidLogged resolve isso com ASM em `WorldRenderer.updateRenderer`, que o Angelica
não usa: sob o Angelica 2.2.43 o fluido de um bloco fluidlogged não é desenhado.

**Opções avaliadas.**

| Opção | Como | Avaliação |
|---|---|---|
| V1 | blocos que podem conter água passam a responder `canRenderInPass(1) == true`; em `renderBlockByRenderType`, se o passe é 1 e o bloco não é do passe 1, desenha **só** a água (se o bit está ligado) e retorna | funciona igual nos dois pipelines; nenhum mixin no laço do Angelica; custo: cada bloco candidato é visitado uma segunda vez com um teste barato |
| V2 | mixin tardio no laço de `AngelicaChunkBuilderMeshingTask` | acopla ao código interno, que muda entre versões: esse arquivo tem 250 linhas de diferença entre 2.2.37 e 2.2.43 |
| V3 | desenhar a água dentro do passe 0 do bloco | a geometria cai no material opaco: água sem transparência |

**Proposta: V1**, com estes detalhes:

* `canRenderInPass` é sobrescrito por blocos de mods. O mixin vai na classe base `Block`; quem sobrescreve fica sem a
  água desenhada (degradação visível, sem quebra). A lista de classes candidatas é a mesma da Camada C.
* No vanilla, o passe 1 só roda se algum bloco do passe 0 devolveu `getRenderBlockPass() > 0`
  (`WorldRenderer.updateRenderer`, `:197`). É preciso um `@ModifyExpressionValue` nessa chamada, com a posição por
  `@Local`. Esse mixin só tem efeito sem o Angelica.
* A água é desenhada chamando `renderBlockLiquid(Blocks.water, x, y, z)`. As leituras internas (metadata da própria
  célula, vizinhos, altura dos cantos, faces) passam pelas costuras de leitura do
  [ADR-0001](ADR-0001-armazenamento-waterlogging.md), que respondem "água fonte" para a célula com o bit.
* **Não embrulhar o `IBlockAccess`**: o Angelica faz `instanceof WorldSlice` em cinco pontos (luz suave, cache de luz
  de fluido). Um wrapper desligaria esses caminhos.
* Shaders: em volta do desenho, `Iris.setShaderMaterialOverride(Blocks.water, 0)` e `resetShaderMaterialOverride()`
  (métodos públicos do Angelica, presentes em 2.2.37, 2.2.43 e 2.2.45; chamados por reflexão), para o shaderpack
  tratar os vértices como água.
* A face lateral da água fica recuada `0.001` (comportamento do `renderBlockLiquid`), então a parte coberta pela
  laje fica atrás dela. Não há formas de oclusão no 1.7.10; aceitar esse resultado na primeira versão.

**Dado na thread de meshing.** A cópia da seção já traz a camada (o Angelica clona pelo ChunkAPI). Falta alcançá-la a
partir do `IBlockAccess`: o `WorldSlice` guarda as cópias num array privado. É preciso um mixin tardio de **acesso**
(`@Accessor` para `sections`, `baseX/Y/Z`), no mesmo estilo dos que o BelowCore já tem em `WorldSlice`. O arquivo
`WorldSlice.java` é idêntico em 2.2.37, 2.2.43 e 2.2.45, então o alvo é estável nas versões em uso. Se ele não
aplicar (versão nova do Angelica), o mod registra um aviso e não desenha a água de blocos waterlogged sob o Angelica;
a física continua correta.

Caminho de longo prazo: propor ao Angelica um ponto de extensão neutro (hoje a integração é específica do FluidLogged).

## 5. O que fica para depois

* `water_overlay` nas faces contra vidro e folhas (G-8).
* Verso do topo da água (G-9).
* Partículas e sons novos.
* LODs do DistantHorizons.

## Verificações que fecham este ADR

* **[A VERIFICAR]** em jogo: Angelica sem shader, com shader (dois packs), e vanilla puro, as quatro combinações de
  cor + névoa.
* **[A VERIFICAR]** o `@Surround` do Angelica em `renderBlockLiquid` aceita a chamada com `Blocks.water` numa célula
  cujo bloco é outro (o Et Futurum já faz isso com a coluna de bolhas, o que é um bom sinal).
* **[A VERIFICAR]** `LightDataCache` do Angelica (ramo `block.getMaterial().isLiquid()`): a luz da água de um bloco
  waterlogged.
* **[A VERIFICAR]** blocos com `ISimpleBlockRenderingHandler` próprio chamados no passe 1 sem esperar.
* **[A VERIFICAR]** ordem de ordenação translúcida do Celeritas com água dentro de blocos.
* **[A VERIFICAR]** `BiomeEvent.GetWaterColor` com biomas do Biomes O' Plenty e outros mods do pack.
