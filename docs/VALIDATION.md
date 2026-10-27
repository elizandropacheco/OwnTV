# Validação local

## Base funcional 0.2.2

Verificação em 25/10/2026:

- `:app:assembleRelease -PlocalSign`: compilação otimizada e assinatura local.
- `:app:lintRelease`: análise Android sem erros.
- RSS dos três temas iniciais: respostas válidas e itens encontrados.
- SSH: execução de comando autenticado; SFTP: escrita/leitura e remoção do arquivo de teste; senha incorreta rejeitada.
- Provedor Android TV consultado com as permissões do OwnTV: sete recomendações do YouTube encontradas. A versão final exibiu `Recomendações do YouTube`, `1–4 de 7`, com quatro miniaturas completas na primeira linha. Corrigido o orçamento global que podia excluir fontes posteriores; agora há limite por aplicativo.
- Favoritos: foco inicial no menu, ↓ direto ao primeiro favorito, ↑ em Câmeras, ↑ em PS5 e ↑ ao menu, conferidos na TV. Alteração/restauração de ordem e troca entre ícones coloridos/monocromáticos testadas. Cores por marca selecionadas como padrão.
- Paginação YouTube pelo D-pad: `1–4 de 7` → `5–7 de 7` → `1–4 de 7`. Duração fornecida para os sete itens; canal público obtido para seis. O item sem autor permanece identificado pela fonte. Rodapé compactado.
- Botão Home físico: usuário confirmou retorno/permanência no OwnTV sem exibir o launcher original. Comandos ADB injetados não foram tratados como prova equivalente da tecla física.
- Abertura: logo EP centralizado, progressão de escala/opacidade e retorno à interface conferidos por gravação na TV. A renderização foi consolidada em Canvas para evitar a camada vazia observada nessa TCL.
- Identidade: EP e monograma OwnTV alternados na Home/menus e EP restaurado. Imagem personalizada depende de arquivo válido via seletor Android.
- Câmeras: mosaico e fluxo principal ao vivo em 1920×1080, preenchimento e contorno arredondado conferidos. Capturas privadas não são publicadas.
- Download agendado: duas novas fotos válidas, total de 17 locais nessa execução.
- Interface verificada na TCL Android TV 14: composição aberta, foco sem escala, quatro sugestões por página e notícias de duas linhas.

Não são medições universais de desempenho ou garantia de compatibilidade com todo firmware. LG/webOS é um alvo futuro.

A configuração de CI está em [ci/build.workflow.yml](ci/build.workflow.yml). A conexão utilizada na publicação não possui escopo para criar workflows no GitHub; esse arquivo é um modelo, não um workflow ativo.

## Entrega 0.2.3

A entrega 0.2.3 reorganiza documentação e scripts, padroniza a formatação dos fontes e troca os recursos de marca por PNGs renderizados. Os resultados atuais de build, lint, manifesto e assinatura ficam em `releases/0.2.3/build-info.json`. Os testes funcionais da seção anterior correspondem à 0.2.2; não são apresentados como uma nova rodada integral de testes da 0.2.3.

A integração IoT/Alexa foi estudada e documentada, mas não foi implementada nem testada em dispositivos.

Na revisão de segurança da 0.2.3, Kotlin/R8/Lint foram atualizados e as dependências de build corrigidas. A política SSH e a sanitização de imagens possuem testes locais reexecutáveis. Consulte [SECURITY_REVIEW.md](SECURITY_REVIEW.md) e o manifesto de segurança da release para os resultados finais.

## Android 0.2.4 e piloto LG 0.1.1 — 27/10/2026

Android 0.2.4 acrescenta Entradas ao rodapé, reutilizando o seletor de `TvInputManager`. Compilação release, lint, manifesto e assinatura são registrados em `releases/0.2.4/build-info.json`. Esta revisão Android não foi instalada na TCL durante a rodada no escritório; a validação física anterior de Home se refere à versão anterior.

Para LG, consulte o [registro das seis unidades](../releases/webos/0.1.1/README.md). Cada revisão do piloto deve ser conferida em todas as TVs participantes e terminar com a Home aberta para comparação visual.
