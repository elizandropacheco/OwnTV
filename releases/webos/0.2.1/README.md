# OwnTV webOS 0.2.1 — aplicativos favoritos

Release experimental para LG webOS, separada do Android 0.2.4.

## Entrega

- Catálogo de aplicativos fornecido pela própria LG, com pareamento local e chave privada por TV.
- Selecionar/remover favoritos com OK e ordenar com setas, incluindo movimentação entre páginas.
- Ícones nativos coloridos, modo monocromático e iniciais se uma imagem falhar.
- Nove apps por página no seletor e quatro favoritos completos por grupo na Home.
- Lista vazia permitida, migração das preferências anteriores e persistência local.

[Como selecionar, ordenar e parear](../../../docs/LG-FAVORITOS.pt-BR.md) · [Conta LG e Developer Mode](../../../docs/LG-WEBOS.pt-BR.md) · [Estado e pendências](../../../docs/STATUS.pt-BR.md).

## Download e build

Baixe `io.elizandropacheco.owntv.webos_0.2.1_all.ipk`, `SHA256SUMS`, `build-info.json` e `validation.json` na [release webos-v0.2.1](https://github.com/elizandropacheco/OwnTV/releases/tag/webos-v0.2.1). Confira o hash antes de instalar. O código permanece aberto em `webos/`, sem ZIP obrigatório; `scripts/build-webos.sh` produz o IPK nesta pasta.

Instale por cima da versão anterior; não desinstale para atualizar, pois as preferências e o pareamento são locais. As seis TVs de validação receberam o pacote com o mesmo SHA256 da publicação.

## Validação de 27/10/2026

- Seis LG 43LM631C0SB, firmware 05.40.97/SDK 4.10.0, Chromium 53 e serviço Node 0.12.2.
- Catálogo real e pareamento em cada TV; nenhuma lista particular foi incluída no pacote.
- 23 verificações funcionais da interface por unidade: seleção, remoção, ordem nas quatro direções, limites, paginação, foco e persistência após recarregar.
- A verificação inicial de imagens amostrou algumas antes de terminar o carregamento. A conferência posterior confirmou os nove ícones da primeira página em todas, sem substituição por iniciais. O manifesto preserva os dois resultados.
- Os ícones e os limites de tela da segunda página também passaram nas seis.
- Estado vazio e reabertura do seletor passaram nas seis; as escolhas anteriores foram restauradas após os testes.
- Chaves de pareamento e configurações privadas de câmeras permaneceram presentes após a atualização; o serviço confirmou versão 0.2.1 nas seis.
- Abertura do YouTube confirmada pelo aplicativo em primeiro plano nas seis. Netflix abriu em quatro; em duas a LG levou à Content Store. O motivo do redirecionamento não foi confirmado.
- Testes locais de compatibilidade ES5, modelo, descoberta, conteúdo e isolamento do serviço passaram. Guarda de arquivos públicos, Gitleaks e busca dos valores privados reais no código/IPK não encontraram correspondências.

Confira [validation.json](validation.json). Os testes de navegação foram automatizados nos aparelhos reais; não equivalem a teste de todas as teclas do controle físico, reinicialização elétrica ou operação contínua por horas. Não foi repetida a rodada de vídeo de câmeras nesta entrega.

## Home continua LG

O botão físico Home **não foi substituído**. Nas seis TVs a API `setDefaultApp` respondeu `Unknown method`. A versão não instala auxiliar nativo, root ou interceptador de teclas. A viabilidade de outros métodos permanece sem validação nesse hardware.
