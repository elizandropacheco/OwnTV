# OwnTV para LG webOS — piloto 0.1.1

Implementação inicial em HTML, CSS e JavaScript compatível com o motor Chromium 53 das LG de referência. Reaproveita a identidade visual e os recursos renderizados do Android; não executa o APK nem contém todas as funcionalidades Android.

## Disponível neste piloto

- Home aberta com o slogan, menu Início/Filmes/Música/Jogos/Casa/Todos os apps e dois cards laterais, seguindo a composição Android.
- Logo EP por padrão, monograma OwnTV opcional e abertura animada.
- Quinze fotografias da coleção Android, preparadas para Full HD, com créditos e licenças; escolha de fundo, cinco níveis de escurecimento e alternância ativada por padrão a cada um minuto enquanto o app está visível; pode ser desligada nas configurações.
- Favoritos com ícones coloridos por padrão ou monocromáticos, ordem configurável e persistência local.
- Configurações em três colunas e página Sobre sem rolagem horizontal.
- Superfícies translúcidas com bordas em gradiente separadas do preenchimento; o foco não movimenta nem amplia o card.
- Botão Entradas no rodapé e nos favoritos abre quatro opções: HDMI 1, HDMI 2, HDMI 3 e Central de entradas LG.
- Atalhos YouTube, TV ao vivo, entradas e HDMI 1–3; a disponibilidade depende dos apps/entradas da TV.
- Navegação: ↓ no menu vai ao primeiro favorito; ↑ nos favoritos vai a Câmeras, depois Jogos e menu. Os controles permanecem no lugar ao receber foco.

## Limites explícitos

- Este é um aplicativo aberto pelo menu LG. Não substitui Home, não inicia automaticamente ao ligar a TV e não fornece root/SSH permanente.
- A lista de apps é a seleção de atalhos do piloto, não um inventário de aplicativos instalados. O modo desenvolvedor negou as APIs de inventário geral nos testes.
- Os menus Música e Casa informam o estado da integração. O card Câmeras ainda não reproduz vídeo. Não foram importadas configurações reais de câmeras.
- Notícias, sugestões de conteúdo, descoberta de apps, imagens personalizadas, download diário de fundos e servidor SSH próprio ainda não foram portados.
- Navegador, URL inicial, abertura automática de sites e tela cheia de páginas foram retirados do escopo a pedido do usuário. O navegador LG não renderizou corretamente o site moderno testado; o projeto não instala Chrome.

## Compilar

Instale Node.js/npm e a [CLI oficial LG](https://webostv.developer.lge.com/develop/tools/cli-installation). Para gerar os derivados das fotos, instale Pillow em um ambiente Python próprio. Exemplo para macOS/Linux:

```sh
npm install -g @webos-tools/cli
python3 -m venv .tools/webos-assets
.tools/webos-assets/bin/pip install Pillow
python3 scripts/fetch-wallpapers.py --count 15
.tools/webos-assets/bin/python scripts/prepare-webos-assets.py
./scripts/build-webos.sh
```

O preparador usa as fotos do catálogo Android, mantém a proporção, limita as dimensões a 1920×1080 e remove metadados de captura. Os JPEGs são gerados localmente, não versionados. O pacote contém as fotos e os créditos. A opção `--source /pasta/das/fotos` aceita uma coleção previamente baixada pelos scripts do projeto. Caso a CLI não esteja no PATH, defina `OWNTV_ARES_PACKAGE` com o caminho de `ares-package`.

Saída: `releases/webos/0.1.1/`, com IPK, SHA-256 e metadados. O APK Android é compilado separadamente; a versão 0.2.4 também acrescenta Entradas ao rodapé da Home.

## Instalar, testar e remover

Primeiro siga [conta LG, Developer Mode e chave SSH](../docs/LG-WEBOS.pt-BR.md). Cadastre uma TV-piloto na CLI com nome `lg-piloto`:

```sh
ares-install --device lg-piloto releases/webos/0.1.1/io.elizandropacheco.owntv.webos_0.1.1_all.ipk
ares-launch --device lg-piloto io.elizandropacheco.owntv.webos
ares-inspect --device lg-piloto io.elizandropacheco.owntv.webos
```

A sequência é a via oficial de distribuição. Na instalação de referência foi usado SFTP e o serviço público de instalação do modo desenvolvedor. Consulte o [registro de validação](../releases/webos/0.1.1/README.md) para distinguir instalação, renderização e testes funcionais.

Para sair, pressione Home no controle. Para remover apenas o piloto:

```sh
ares-install --device lg-piloto --remove io.elizandropacheco.owntv.webos
```

Remover o app apaga suas preferências locais. Desativar ou deixar expirar Developer Mode também pode remover aplicativos de desenvolvimento. O launcher LG permanece instalado.

## Estrutura e recursos

- `app/`: pacote webOS, metadados, páginas, estilos e JavaScript ES5.
- `app/model.js`: normalização e persistência das preferências.
- `app/assets/wallpapers.json`: fontes, autores, licenças e transformação das fotos.
- `app/vendor/`: webOSTV-dev.js 1.2.13, da LG, sob Apache-2.0; licença incluída. Origem: https://webostv.developer.lge.com/assets/library/webOSTVjs-1.2.13.zip.
- `tests/model.test.cjs`: estado corrompido, padrões, ordenação, duplicatas e persistência.

O código OwnTV segue a licença do projeto. Logos pessoais são somente derivados renderizados. Os símbolos YouTube e PlayStation vêm dos mesmos recursos Simple Icons da versão Android; marcas continuam pertencendo aos titulares. As fotografias preservam suas licenças, incluindo atribuição e compartilhamento pela mesma licença quando aplicável.

No Chromium antigo, recursos `file:` precisam ser explicitamente permitidos pela política do pacote; `'self'` sozinho não carregou CSS/JS no aparelho. A política continua bloqueando rede, frames, objetos e execução de scripts remotos. Não remova a política para contornar problemas de carregamento.

## Conferência em várias TVs

Em cada revisão, instalar e abrir o mesmo pacote em todas as TVs participantes, sem limitar a validação a uma unidade. Aguardar a instalação concluir antes de abrir: uma solicitação de instalação aceita ainda pode estar em processamento.

Em cada aparelho, conferir Home, carregamento das imagens, navegação do menu aos favoritos e de volta aos cards, Configurações, persistência das preferências e Entradas. Verificar os limites dos botões na tela, inclusive com foco. Testar as três HDMI e a central LG, depois retornar à Home do OwnTV para comparação visual. Registrar versão, checksum, verificações e falhas sem publicar IPs, chaves ou dados das câmeras.

Os testes automatizados de navegação enviam eventos ao aplicativo real pelo depurador de desenvolvimento. Isso complementa, mas não substitui, a conferência com o controle físico. Um comando HDMI aceito pela TV não comprova sinal de um aparelho conectado.
