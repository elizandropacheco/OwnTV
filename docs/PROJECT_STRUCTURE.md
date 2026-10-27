# Estrutura do código

OwnTV é um aplicativo Android em Kotlin e Java, baseado no Couchy Launcher sob GPLv3. Há um módulo Android, `app`. Este mapa descreve arquivos reais; não presume módulos que ainda não existem.

```text
OwnTV/
├── app/
│   ├── build.gradle.kts             # versão, SDK, assinatura, dependências
│   ├── proguard-rules.pro           # otimização e reflexão
│   └── src/main/
│       ├── AndroidManifest.xml      # atividades, serviços, permissões
│       ├── java/com/conreo/couchytv/
│       │   ├── MainActivity.kt      # entrada da Home e ciclo de vida
│       │   ├── EpActivity.java      # páginas nativas de configuração
│       │   ├── OwnBrand.java        # identidade e abertura
│       │   ├── OwnCameraPanel.java  # mosaico e vídeo RTSP
│       │   ├── OwnHomeService.java  # acessibilidade/Home
│       │   ├── OwnSshService.java   # SSH/SFTP no UID do app
│       │   ├── OwnBootReceiver.java # retomada de serviços habilitados
│       │   ├── OwnWallpaperJob.java # downloads agendados
│       │   ├── OwnWallpapers.java   # catálogo e arquivos de wallpapers
│       │   ├── data/                # configurações, apps e notícias
│       │   └── ui/                  # Compose e conteúdo compartilhado
│       ├── res/                     # textos, temas, layouts e imagens
│       └── assets/                  # catálogos, créditos e recursos
├── webos/                           # piloto LG separado, interface e testes
├── scripts/
│   ├── build-release.sh             # entrada Bash da entrega
│   ├── package-release.py           # build, lint e empacotamento
│   ├── check-public-assets.py       # verifica arquivos não publicáveis
│   └── fetch-wallpapers.py          # download opcional de fotografias
├── releases/                        # outputs e metadados por versão
├── docs/                            # instalação, build e validação
├── fastlane/metadata/               # descrição e changelogs Android
├── gradle/wrapper/                  # Gradle controlado pelo projeto
├── build.gradle.kts                 # versões dos plugins
├── settings.gradle.kts              # módulos e repositórios Maven
├── gradle.properties                # opções de build
├── CHANGELOG.md                     # mudanças por versão
├── CONTRIBUTING.md                  # contribuição
├── LICENSING.md                     # créditos e componentes
└── LICENSE                          # GPLv3
```

## Interface e estado

`ui/OwnTVHome.kt` monta menu, cards, favoritos, sugestões, relógio e foco do controle. `ui/OwnMedia.kt` lê conteúdo compartilhado pelo Android TV. `ui/OwnYouTubeMetadata.kt` complementa nomes públicos de autores quando disponíveis, sem acessar histórico privado da conta.

`data/Config.kt` e os componentes da base cuidam da configuração do launcher. Funcionalidades OwnTV também usam preferências privadas por assunto, como `own_brand`, `own_cards` e `own_news`. `data/AppRepository.kt` descobre aplicativos/ícones e `data/OwnNews.kt` consulta e armazena temporariamente manchetes.

`EpActivity.java` concentra páginas nativas e continua uma classe grande. Uma extração futura deve separar responsabilidades preservando foco, navegação e migração de dados. A organização de entrega não representa uma reescrita dessa arquitetura.

## Sistema e serviços

`OwnHomeService.java` integra a tecla Home por acessibilidade em firmwares compatíveis, sem capturar o conteúdo da tela. O teste com controle físico é necessário.

`OwnCameraPanel.java` usa Media3 para RTSP e limita as prévias simultâneas. `OwnSshService.java` usa um processo separado com as permissões do aplicativo, sem root ou equivalência ao ADB.

Esses componentes não representam uma implementação IoT/Alexa. Integrações futuras exigem código, configuração e validação próprios.

## Recursos públicos e privados

Imagens necessárias para executar o aplicativo são públicas. Logos são distribuídos apenas como imagens renderizadas; originais editáveis e desenhos vetoriais privados ficam fora da árvore pública. Marcas de terceiros mantêm suas atribuições.

Fotografias grandes e artes opcionais locais ficam fora do Git, com catálogos/créditos públicos. Dados de DVR, SSH, rede, chaves e backups nunca pertencem ao código ou aos exemplos.

## Build e distribuição

`app/build/` contém intermediários do Gradle. A entrega verificada fica em `releases/<versão>/`, com APK anexado à GitHub Release e código/metadados navegáveis. Veja [Build](BUILDING.md), [Instalação](INSTALLATION.pt-BR.md) e [Validação](VALIDATION.md). O modelo em `docs/ci/` só será ativo se configurado em `.github/workflows/`.

## Piloto LG webOS

`webos/app/` contém a implementação inicial para LG, independente do módulo Android. `scripts/prepare-webos-assets.py` prepara fotos Full HD e `scripts/build-webos.sh` gera o IPK em `releases/webos/<versão>/`. Veja [limites e instalação](../webos/README.md). O módulo Android e sua versão não mudam com uma entrega experimental webOS.

## Conteúdo e câmeras LG 0.2.0

- `webos/app/content-ui.js`: notícias, sugestões, paginação e preferências de fontes.
- `webos/app/cameras.js`: mosaico JPEG e player HLS, foco e ciclo de vida.
- `webos/service/`: serviço Luna/Node ES5; RSS por HTTPS, cache limitado e configuração privada da ponte. Métodos de conteúdo e credenciais aceitam somente o app OwnTV.
- `webos/bridge/`: servidor Python/FFmpeg, autenticação por token e IP e instalador opcional de LaunchAgent macOS.
- `webos/tests/`: modelos de preferências, parsers RSS e limites de acesso HTTP.

Dados reais de câmeras, tokens e chaves não fazem parte desses diretórios. A configuração da ponte fica fora do checkout; `config.example.json` contém somente exemplos substituíveis.

## Favoritos no webOS

- `webos/app/model.js`: migração de preferências, catálogo, seleção e ordem.
- `webos/app/apps-discovery.js`: consulta SSAP à própria TV, com pareamento persistido no serviço.
- `webos/app/app.js`: telas de apps/favoritos, paginação e foco do controle.
- `webos/service/service.js`: isolamento e armazenamento privado do pareamento.
- `webos/tests/apps-discovery.test.cjs`: respostas inválidas, chave revogada e restrição ao endereço da TV.

As instruções para o usuário ficam em [LG-FAVORITOS.pt-BR.md](LG-FAVORITOS.pt-BR.md).
