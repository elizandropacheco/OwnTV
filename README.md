# OwnTV

![OwnTV](docs/branding/owntv-wordmark.png)

**Sua TV. Suas regras.** Uma central de entretenimento, jogos e câmeras que coloca a personalização nas suas mãos.

[![License: GPL v3](https://img.shields.io/badge/license-GPLv3-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%20TV-2CEB43)](#compatibilidade)
[![Validação local](https://img.shields.io/badge/build-verificado_localmente-2CEB43)](docs/VALIDATION.md)
[![Version](https://img.shields.io/badge/version-0.2.4-0084FE)](https://github.com/elizandropacheco/OwnTV)

[English](README.en.md) · [Desenvolvedor](https://elizandropacheco.io/) · [Código](https://github.com/elizandropacheco/OwnTV)

OwnTV transforma a tela inicial em um espaço organizado para seus aplicativos, console, conteúdos e câmeras. A composição aberta deixa o wallpaper aparecer; o menu superior, o dock e os efeitos de foco ajudam a navegar pelo controle remoto.

## Por que usar

- **Home sem publicidade:** sem banners patrocinados, serviços de anúncios ou feed comercial do launcher.
- **Personalização real:** favoritos, ordem, categorias, aplicativos ocultos, cards, títulos, ações, imagens, enquadramento e transparência.
- **Foco estável e leveza:** imagens decodificadas para a resolução útil da interface, carregamento em segundo plano, listas de conteúdo sob demanda e transições curtas de cor. A velocidade depende do hardware; o projeto não promete um ganho percentual sem medição.
- **Entradas na Home:** botão fixo no rodapé abre o seletor HDMI/dispositivos no Android e no piloto webOS.
- **Central de jogos:** PS5 por HDMI configurável, Game Master, ajustes de imagem, entradas e orientação para 120 Hz/120 FPS.
- **Casa conectada:** mosaico de câmeras com rolagem, foco pelo controle, imagem ampliada pelo fluxo principal e atalhos para aplicativos Intelbras e iCSee.
- **Paisagens offline:** coleção inicial de 15 fotos acima de 4K, catálogo de 100 fotos, rotação, downloads diários e créditos completos.
- **Acesso pela rede:** servidor SSH e SFTP com usuário, senha e porta configuráveis, sem exigir ADB para cada conexão.
- **Liberdade e reversão:** código aberto baseado no Couchy Launcher. O launcher e as configurações originais do Android continuam disponíveis.

A identidade EP usa fundo `#030B12`, azul `#0084FE`, verde `#2CEB43`, linhas geométricas, superfícies translúcidas, contornos em gradiente e transições suaves de foco.

## Compatibilidade

Validado em uma TCL com Android TV 14. Os atalhos específicos da TCL dependem do firmware e da entrada HDMI. O OwnTV é um launcher Android: a implementação LG/webOS é separada e não faz parte deste APK.

**LG/webOS:** há um [piloto separado 0.1.1](webos/README.md), com Home no estilo Android, fundos, menus, personalização e atalhos. Não possui todas as funções Android nem substitui a Home LG. Consulte [conta LG Developer, correção de país, Developer Mode, SSH e hardware](docs/LG-WEBOS.pt-BR.md). O acesso de desenvolvedor foi verificado em seis LG 43LM631C0SB; o piloto foi instalado e teve interface e navegação verificadas nas seis unidades. Root e SSH permanente permanecem não validados. Navegador e abertura de URLs estão fora do escopo. As seis usam relógio automático no fuso de São Paulo e SSH restrito com shell; usuário SSH extra não foi criado por falta de privilégios. Confira [horário, acesso e versão do sistema](docs/LG-WEBOS.pt-BR.md#relógio-acesso-e-versão-do-sistema--conferência-de-27102026).

Os arquivos dos wallpapers mantêm a resolução original. A resolução da interface é determinada pelo Android e pode ser inferior à resolução física do painel. O launcher adapta a decodificação à interface para evitar consumo desnecessário de memória.

## Instalar

**[Guia completo: APK, modo desenvolvedor, depuração ADB, instalação, configuração e reversão](docs/INSTALLATION.pt-BR.md)** · **[Baixar versões](https://github.com/elizandropacheco/OwnTV/releases)**

1. Faça backup das configurações do launcher atual. Preserve seus APKs e um caminho de acesso às configurações do Android.
2. Baixe o APK da [release atual](https://github.com/elizandropacheco/OwnTV/releases/latest) ou compile com o script abaixo e permita sua instalação na TV.
3. Instale por um gerenciador de arquivos ou, com ADB já autorizado:

```sh
adb connect TV_IP:PORTA_ADB
adb install -r OwnTV-0.2.4.apk
adb shell am start -n io.elizandropacheco.tv/com.conreo.couchytv.MainActivity
```

4. Se o Android permitir, escolha OwnTV como aplicativo de início. Em firmwares TCL que insistem no launcher original, ative **OwnTV Home** em **Configurações → Acessibilidade** e permita a inicialização automática do aplicativo nas opções da TCL.
5. Para o conteúdo compartilhado pelos aplicativos, conceda a permissão de leitura de listas da TV quando disponível. Pela instalação ADB:

```sh
adb shell pm grant io.elizandropacheco.tv android.permission.READ_TV_LISTINGS
```

O serviço OwnTV Home intercepta somente a tecla Home quando o firmware permite, para abrir diretamente o OwnTV. Como alternativa, reage à abertura da atividade Home do launcher original. Não captura nem registra o conteúdo das telas. Alguns firmwares fixam a prioridade do launcher original; a disponibilidade da interceptação depende do aparelho.

**Atualizações:** use a mesma chave de assinatura. Desinstalar ou limpar os dados apaga a configuração local e o acesso às câmeras; não faça isso para contornar uma assinatura diferente.

## Personalizar pelo controle

| Onde | O que pode mudar |
| --- | --- |
| **Personalizar apps → Aplicativos** | Mostrar/ocultar apps, abrir informações e desinstalar pelo diálogo do Android |
| **Personalizar apps → Seções** | Criar, renomear, ordenar e preencher categorias; definir favoritos |
| **Personalizar apps → Aparência** | Escala, tamanho dos ícones, espaçamento, cantos, altura dos cards, escurecimento, efeitos, nomes e relógio |
| **Cards e jogos** | Adicionar, remover, ordenar e editar título, descrição, ação, imagem, alinhamento e intensidade dos cards |
| **Imagens dos aplicativos** | Usar uma imagem local/USB ou restaurar a imagem padrão |
| **Configurações → Fundos EP** | Escolher paisagem, alternância, intervalo e downloads diários |
| **Relógio** | Data, fuso da interface e acesso à configuração de data/hora do Android |

As setas movem o foco; **OK** abre o item. Na Home, **↓ no menu superior vai diretamente ao primeiro favorito**. **↑ nos favoritos seleciona Câmeras**, outro ↑ seleciona PS5 e outro ↑ retorna ao menu superior. No sentido inverso, ↓ percorre PS5 → Câmeras → favoritos. No dock, **↓** leva aos conteúdos abaixo. As recomendações usam quatro cards completos por página, com setas para avançar e voltar. O botão de menu do controle abre as configurações do OwnTV quando o firmware entrega essa tecla ao aplicativo.

A seleção de arquivos locais depende de um gerenciador compatível com o seletor de documentos do Android.

## Notícias na Home

Abaixo do slogan, manchetes reais alternam em duas linhas, com espaço reservado para não deslocar a tela. Em **Configurações → Notícias e temas**, ative/desative o bloco, informe de um a oito temas separados por vírgulas e escolha o intervalo de alternância. Os temas iniciais são telecom, tecnologia e inteligência artificial.

As consultas usam o RSS do Google Notícias em português, com fonte identificada e cache local de 30 minutos. Os termos escolhidos são enviados ao serviço para buscar as manchetes. O aplicativo não baixa os artigos completos nem inventa notícias quando está sem conexão.

Em **Configurações → Personalizar sua central → Ordem dos favoritos**, mova cada app antes/depois ou para o início/fim. Ao entrar no dock, o primeiro favorito à esquerda recebe o foco. Em **Personalizar sua central → Ícones dos favoritos**, escolha cores uniformes por marca (padrão) ou o modo monocromático. Artes vetoriais são brancas no modo monocromático; imagens personalizadas e ícones nativos usam tons de cinza. A Home aguarda a arte definitiva em vez de mostrar primeiro um ícone antigo e substituí-lo.

## Conteúdos e YouTube

OwnTV lê os conteúdos que os aplicativos publicam no provedor de TV do Android, como **Continuar assistindo** e canais de recomendações. As linhas respeitam aplicativos ocultos e podem ser desligadas. As miniaturas preservam a proporção 16:9; o rodapé mostra aplicativo/canal e data de publicação, e a duração aparece sobre a imagem quando o aplicativo fornece esses dados. Não são estimadas datas ou durações ausentes. Quando o YouTube não fornece o nome do canal no provedor, o OwnTV consulta somente os metadados públicos do vídeo pelo endpoint oEmbed do YouTube; o cache dura sete dias, há no máximo três consultas simultâneas e os cards aparecem antes dessas consultas terminarem. Nenhuma conta, cookie ou histórico privado é acessado.

A primeira linha de conteúdo é a do YouTube: recomendações publicadas pelo aplicativo ou continuidade, quando disponível. A leitura tem limites por aplicativo, para que uma fonte com muitos itens não elimine as outras. Quando o YouTube não publica vídeos no provedor do Android, o OwnTV mostra um atalho explícito para abrir o aplicativo.

O atalho **YouTube → Inscrições** solicita a página de inscrições ao aplicativo. Os vídeos dos canais inscritos só aparecem diretamente na Home se o aplicativo os compartilhar com o sistema; o launcher não acessa senhas, cookies ou a conta do YouTube.

A ausência de anúncios se refere ao OwnTV. Publicidade dentro do YouTube ou de outros serviços continua sob controle de cada aplicativo.

## PS5, Fortnite e HDMI

Em **Jogos → HDMI do PS5**, escolha a entrada correta. Os atalhos de Game Master, imagem e entradas abrem os controles do aparelho.

Para 120 FPS, confira no PS5 a saída de vídeo a 120 Hz, o modo de desempenho do jogo e a opção de 120 FPS do Fortnite. TV, porta HDMI, cabo, console e jogo precisam oferecer o modo desejado. OwnTV não altera o firmware nem inventa suporte a frequências indisponíveis.

As artes opcionais de jogos são personalização local. O código público inclui uma ilustração de fallback; arquivos e marcas de terceiros têm suas próprias condições de uso.

## Câmeras e DVR

Abra **Câmeras → Opções do DVR → Configurar** e informe host, usuário, senha, porta e quantidade de canais. A implementação nativa usa RTSP no formato Dahua/Intelbras compatível com `cam/realmonitor`.

- O mosaico é rolável e mantém no máximo quatro prévias ativas.
- **OK** abre o fluxo principal, mostrando a resolução recebida.
- **Voltar** restaura canal, foco e posição do mosaico.
- **Enquadrar** oferece proporção original, preenchimento com corte ou esticar. **Preencher** é o padrão.
- **Modo leve** usa o substream quando o principal não for adequado à conexão.

Mibo, iSIC e iCSee também têm atalhos para seus próprios aplicativos. A integração nativa não substitui protocolos proprietários de todos os modelos.

RTSP tradicional não oferece criptografia de transporte: use uma rede local confiável e não exponha o DVR na internet.

As credenciais do DVR ficam criptografadas com AES-GCM e Android Keystore no armazenamento privado da TV. Não entram nas configurações públicas do projeto nem no catálogo de imagens. Este repositório não contém endereços ou acessos de instalações reais.

## SSH e SFTP

Em **Configurações → Acesso SSH e SFTP**, escolha usuário, senha com pelo menos dez caracteres e uma porta entre 1024 e 65535. Salve para iniciar. O serviço aceita autenticação somente de endereços privados, locais ou de loopback; use apenas uma rede confiável. A porta sugerida é 2222; o status e o endereço aparecem nessa página. O serviço mantém uma notificação enquanto está ativo e volta após a inicialização quando habilitado.

```sh
ssh -p 2222 SEU_USUARIO@TV_IP
sftp -P 2222 SEU_USUARIO@TV_IP
```

A senha não é guardada em texto: usa PBKDF2-HMAC-SHA256 com salt individual. A chave do servidor permanece na TV. O terminal executa como o usuário do OwnTV, com as permissões normais do Android, sem root ou permissões equivalentes ao ADB. O SFTP inicia numa pasta própria para transferência de arquivos. Encaminhamento de portas está desativado.

Use **Parar servidor SSH** para desativar o acesso e o reinício automático. Usuário, senha e dados reais da rede não devem ser publicados em issues, exemplos ou commits.

## Wallpapers e relógio

Os downloads usam um catálogo finito de fotografias com fontes e licenças registradas em `app/src/main/assets/ep/wallpapers/catalog.json`. A TV guarda os originais na área privada do aplicativo. A configuração permite 1, 3 ou 5 novas fotos por dia e limites de 25, 50 ou 100. O agendamento depende de conexão, espaço e decisões do Android; a TV desligada não faz downloads.

A coleção continua funcionando offline. A rotação pausa quando a Home não está visível; apenas a imagem corrente e a transição precisam ficar decodificadas.

O relógio pode acompanhar o fuso do sistema ou usar um identificador IANA. Sincronização NTP pertence ao Android. Na TCL configurada, foi usado `a.ntp.br`; outras instalações podem ajustar isso pelas ferramentas e permissões disponíveis no aparelho.

## Código e entregas

O código-fonte está disponível diretamente neste repositório, organizado por responsabilidades, sem depender de um pacote ZIP. Veja o [mapa do projeto](docs/PROJECT_STRUCTURE.md), o [guia completo de build e assinatura](docs/BUILDING.md) e o [guia de contribuição](CONTRIBUTING.md).

- `app/`: aplicativo Android, componentes nativos, interface e recursos.
- `scripts/`: build, validação dos arquivos públicos e download de wallpapers.
- `releases/`: índice das versões, hashes e metadados; o build gera o APK nessa mesma pasta localmente.
- `docs/`: instalação, arquitetura, validação e [estudo de IoT e Alexa](docs/IOT-ALEXA.md).

Os originais editáveis dos logos não são distribuídos. O aplicativo e a documentação usam PNGs renderizados. Veja [a política de identidade](docs/BRANDING.md).

## Compilar

Requisitos: JDK 21, Android SDK 34, Build Tools 35.0.0 e acesso aos repositórios de dependências.

```sh
git clone https://github.com/elizandropacheco/OwnTV.git
cd OwnTV
python3 scripts/fetch-wallpapers.py --count 15
./gradlew :app:assembleDebug
```

O script baixa fotografias em resolução completa, preserva os pixels e retira metadados de captura e localização; os créditos permanecem no catálogo. Os binários grandes são obtidos da fonte e não precisam fazer parte do histórico Git. Para uma compilação local sem downloads, o launcher mantém o fundo vetorial.

```sh
./scripts/build-release.sh
```

O script compila, executa lint, valida o pacote e grava APK, checksum e `build-info.json` em `releases/<versão>/`. A compilação release é sem assinatura por padrão. As variáveis `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` e `KEY_PASSWORD` configuram uma assinatura própria. Não envie chaves, senhas, backups de aparelhos ou perfis reais para o repositório.

## IoT e Alexa — proposta

O [estudo técnico](docs/IOT-ALEXA.md) recomenda OwnTV como painel e ponte leve, Home Assistant em um host sempre ligado e Alexa pela integração Cloud inicialmente. Inclui alternativas, protocolos, arquitetura, limites do standby da TCL, segurança, custos e testes. Estes recursos IoT são um roteiro, ainda não fazem parte do APK.

## Reverter

Desative **OwnTV Home** na acessibilidade e escolha o launcher original como início. O acesso ao Google TV original também está em **Configurações**. Se o OwnTV tiver sido escolhido como Home pelo Android, remova essa preferência nas configurações do sistema. Nenhum pacote do fabricante precisa ser removido.

## Desenvolvimento e licença

Desenvolvido por **[Elizandro Pacheco](https://elizandropacheco.io/)**. Baseado no **[Couchy Launcher](https://github.com/conreo/couchy-launcher)**, de Conreo, sob **GPLv3**. A licença, os créditos e a referência ao projeto original são preservados.

Fotografias mantêm as licenças individuais do catálogo e dos créditos. Os caminhos de logos usados pelo Simple Icons são CC0; marcas continuam pertencendo aos respectivos titulares. Veja [LICENSING.md](LICENSING.md).

[YouTube](https://www.youtube.com/@ElizandroPacheco) · [Instagram](https://www.instagram.com/elizandropacheco/) · [GitHub](https://github.com/elizandropacheco)

## Acompanhe e apoie

Desenvolvido por **Elizandro Pacheco** — [elizandropacheco.io](https://elizandropacheco.io/). Para mais conteúdos de tecnologia, [siga no Instagram](https://www.instagram.com/elizandropacheco/) e [inscreva-se no YouTube](https://www.youtube.com/@ElizandroPacheco). Se o OwnTV ajudou sua TV, [dê uma estrela no projeto no GitHub](https://github.com/elizandropacheco/OwnTV).

[Identidade visual e personalização](docs/BRANDING.md) · Seu logo EP é o padrão da Home, dos menus, da abertura e da identidade do aplicativo. Em **Configurações → Personalizar sua central → Identidade visual e logo**, escolha EP, o monograma OwnTV ou uma imagem local/USB para Home, menus e abertura. A animação aparece na primeira abertura do processo; não se repete a cada retorno pelo botão Home. O ícone do aplicativo instalado mantém a assinatura EP.
