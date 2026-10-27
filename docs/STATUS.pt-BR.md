# Estado do projeto e pendências

Atualizado em **27/10/2026**, após a entrega Android **0.2.4** e LG webOS **0.2.1 experimental**. Este documento separa implementação, validação e itens ainda não atendidos. Não é uma promessa de equivalência entre as plataformas.

**O objetivo de substituir a tela inicial das LG ainda não foi atendido. O botão Home físico abre a Home LG. A seleção e a ordenação dos apps favoritos foram implementadas no webOS 0.2.1.**

## O que está entregue

| Solicitação | Android | LG webOS 0.2.1 |
| --- | --- | --- |
| Home aberta, fundos, menus, identidade EP/OwnTV e abertura animada | Implementado; testes anteriores na TCL | Implementado e conferido nas seis LG |
| Favoritos coloridos e ordem configurável | Lista de aplicativos e personalização | Catálogo da própria TV, seleção/remoção, ícones coloridos e ordenação com controle; configuração por TV |
| Botão Home abrindo diretamente OwnTV | Confirmado pelo usuário no controle físico em versão anterior; firmware dependente | **Não implementado**; abre LG |
| Entradas/HDMI | Incluído no APK 0.2.4; teste físico desta revisão pendente | HDMI 1–3 e central de entradas LG |
| Notícias abaixo do slogan, por temas | Implementado, com configuração de intervalo | Manchetes reais, fonte/data, temas configuráveis, página própria; alternância de 12 segundos |
| Quatro sugestões completas, com metadados | Conteúdo compartilhado pelos apps Android; duração quando fornecida | Vídeos públicos dos canais YouTube escolhidos, com canal e data; não usa histórico/recomendações pessoais |
| Câmeras | RTSP nativo, principal/leve e preenchimento | Ponte externa; seis prévias JPEG em três colunas e um vídeo HLS ampliado por TV |
| Enquadramento e controles de câmeras | Implementado | Proporção preservada, preencher/ajustar, áudio, pausa, reconexão e restauração do foco |
| Wallpapers | Coleção inicial, catálogo e downloads programados | Quinze fundos locais, rotação de um minuto e escurecimento; downloads diários pendentes |
| Logo próprio configurável | EP, OwnTV ou imagem local/USB | Alternância EP/OwnTV; importação de imagem própria pendente |
| SSH | Servidor do app, com permissões do app, sem root | SSH de desenvolvedor `prisoner`; nenhum usuário extra ou acesso permanente |
| Código organizado e builds | Fonte, scripts, APK e metadados publicados | Fonte do app, serviço e ponte; scripts, IPK e metadados publicados |
| Documentação de instalação | Modo desenvolvedor, depuração, assinatura e reversão | Conta LG/país, Developer Mode, chaves, IPK, ponte, hardware e limitações |
| IoT/Alexa | Estudo de arquitetura publicado | Não implementado; a ponte de câmeras não é um gateway IoT/Alexa |

## Home e favoritos: comportamento atual

No **OwnTV**, **Editar favoritos** e **Apps** mostram o catálogo fornecido pela LG. É possível marcar/desmarcar com OK e ordenar os favoritos com as setas. A lista usa nove cards por página; a Home mostra grupos de quatro. TV ao vivo, Entradas e Personalizar são atalhos adicionais. O pareamento e a configuração são individuais por TV. Veja [as instruções completas](LG-FAVORITOS.pt-BR.md).

Dentro do OwnTV, **Voltar** fecha a câmera ampliada e retorna ao mosaico; outro Voltar retorna à página Início. O botão **Home do controle físico** sai para a interface LG. Se o aplicativo estiver fechado ou em segundo plano, abra OwnTV pelo menu LG. O aplicativo não intercepta essa tecla e não inicia automaticamente ao ligar a TV.

A API Luna de inventário continua negada, mas o pareamento SSAP autorizado permitiu obter o catálogo real nas seis LG. As credenciais ficam apenas no serviço privado de cada TV. O teste de definição direta da Home (`setDefaultApp`) retornou **Unknown method** nas seis; nenhum interceptador de teclas foi instalado.

## Pendências em ordem de prioridade

| Prioridade | O que falta | Critério para considerar concluído |
| --- | --- | --- |
| Alta — privacidade | Remover definitivamente os originais dos logos acessíveis por objetos antigos do GitHub | Verificar anonimamente os antigos caminhos/objetos e confirmar indisponibilidade; limpar o `main` ou os novos builds não basta. A cópia privada limpa existe, mas a remoção definitiva ainda está pendente; uma nova conferência anônima nesta data confirmou acessos históricos ainda disponíveis |
| Alta — objetivo principal LG | Substituir Home e definir o comportamento ao ligar a TV | Encontrar e validar uma solução compatível com o firmware, com reversão; confirmar o Home físico vindo de outros apps e após reiniciar. Root não foi aplicado nem validado |
| Alta — câmeras | Ponte independente da sessão manual do Mac | Serviço com rede autorizada que volte após login/reinício, ou migração para servidor sempre ligado; testar queda de rede, retomada e suspensão |
| Média — estabilidade | Validar câmeras por períodos longos e investigar fontes sem prévia | Diferenciar problemas de fonte/rede de falhas do player, medir quedas/reconexões e testar por horas. A rodada atual não certifica operação contínua 24/7 |
| Média — conteúdo | Confirmar abertura do vídeo exato no YouTube; avaliar outros provedores | Conferência na tela real e APIs permitidas. Não inventar duração, histórico ou recomendações ausentes |
| Média — acabamento LG | Importação de imagens/logos, downloads de fundos, intervalos configuráveis e integração de Música | Implementação e testes próprios. Funções exclusivas TCL não devem aparecer como disponíveis nas LG |
| Média — validação Android | Instalar/testar fisicamente o APK 0.2.4 na TCL | Conferir Entradas, Home e regressões com o mesmo pacote publicado; build/lint não substituem esse teste |
| Média — entrega | Ativar a CI no GitHub | Workflow efetivamente configurado e executado; atualmente há apenas o modelo em `docs/ci/` |
| Pesquisa | Usuário SSH extra, SSH permanente e controle explícito de NTP nas LG | Privilégios e método compatíveis demonstrados. Horário automático correto não comprova o servidor NTP utilizado |
| Evolução | IoT/Home Assistant/Alexa | Implementar a arquitetura proposta, autenticação e comandos limitados; testar espera/reinício e operação real |

Nenhuma dessas pendências foi convertida automaticamente em promessa de compatibilidade. Em particular, root não é sinônimo de port Android, SSH permanente ou substituição segura da Home.

## Testes e limites da evidência

A validação de favoritos da versão 0.2.1 está no [registro próprio](../releases/webos/0.2.1/README.md). Os resultados de câmeras abaixo pertencem à rodada 0.2.0 e não significam novo teste contínuo nesta entrega.

- Instalação e atualizações por cima nas seis LG 43LM631C0SB, firmware 05.40.97/SDK 4.10.0; configurações da ponte preservadas.
- 27 verificações de interface por TV, incluindo navegação, notícias/sugestões, configurações, entradas e limites de tela.
- Seis prévias na primeira página e player principal/leve; controles de pausa/áudio, foco ao voltar e proporção. Algumas fontes não responderam na varredura completa.
- O usuário confirmou o desaparecimento do vídeo preto e da imagem achatada. O mosaico passou a três colunas por solicitação dele.
- Uma medição posterior confirmou avanço do vídeo e ausência de erro no player nas seis. Houve carregamentos e reconexões durante a rodada; isso não demonstra ausência de interrupções. Veja o [registro complementar](../releases/webos/0.2.0/follow-up-validation.json).
- A abertura dos cards YouTube foi solicitada nas seis. A inspeção do app em primeiro plano foi negada; o vídeo exato não foi confirmado automaticamente.
- Ponte manual funciona. O LaunchAgent iniciou, mas não alcançou o gravador; foi desativado. A causa observada é compatível com restrição de rede local do macOS, sem autorização operacional resolvida.
- Repositório/artefato atual verificados com guardas de recursos públicos, busca de valores privados, Gitleaks e testes de compatibilidade/segurança. Isso não remove objetos históricos do provedor nem garante ausência absoluta de falhas.

Veja [VALIDATION.md](VALIDATION.md), [manifesto original da release](../releases/webos/0.2.0/validation.json) e [revisão de segurança](SECURITY_REVIEW.md). O IPK 0.2.1 é uma nova entrega; os arquivos históricos 0.2.0 não foram substituídos.

## Itens retirados do escopo ou não suportados

**Navegador, URL inicial e modo quiosque web foram cancelados pelo usuário**, após incompatibilidades do navegador LG. Não são tratados como uma entrega prometida nesta versão.

Não há imagem Android validada para instalar no hardware interno dessas LG. Trocar o sistema operacional continua sem procedimento suportado neste projeto. O port webOS é separado do APK Android.

## Onde está cada instrução

| Assunto | Documento |
| --- | --- |
| Android: instalação, modo desenvolvedor, depuração e reversão | [INSTALLATION.pt-BR.md](INSTALLATION.pt-BR.md) |
| LG: conta, erro de país, Developer Mode, SSH, hardware, firmware e horário | [LG-WEBOS.pt-BR.md](LG-WEBOS.pt-BR.md) |
| LG: pareamento, seleção e ordem de favoritos | [LG-FAVORITOS.pt-BR.md](LG-FAVORITOS.pt-BR.md) |
| LG: recursos, navegação, build e IPK | [webos/README.md](../webos/README.md) |
| Ponte: dependências, arquivo privado, configuração da TV, serviço e desativação | [webos/bridge/README.md](../webos/bridge/README.md) |
| Build Android, assinatura e publicação | [BUILDING.md](BUILDING.md) |
| Estrutura do código e formatação | [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md), [FORMATTING.md](FORMATTING.md) |
| Identidade e limites dos arquivos publicados | [BRANDING.md](BRANDING.md) |
| Testes e segurança | [VALIDATION.md](VALIDATION.md), [SECURITY_REVIEW.md](SECURITY_REVIEW.md) |
| Estudo de IoT/Alexa | [IOT-ALEXA.md](IOT-ALEXA.md) |
| APK/IPK, hashes e registros por versão | [Releases](../releases/README.md) |

IPs reais, Passphrases, tokens, senhas, chaves e imagens das câmeras são deliberadamente omitidos. Documentar a instalação não significa publicar sua configuração privada.
