# OwnTV webOS 0.1.1 — piloto

[Baixar IPK e checksums](https://github.com/elizandropacheco/OwnTV/releases/tag/webos-v0.1.1) · [Instalação e build](../../../webos/README.md) · [Conta LG e Developer Mode](../../../docs/LG-WEBOS.pt-BR.md)

Home inspirada na versão Android, quinze fundos, identidade configurável, favoritos coloridos e reordenáveis, transparência real, bordas em gradiente e seletor de entradas HDMI 1–3/Central LG. Código aberto em `webos/app/`; pacote gerado por `scripts/build-webos.sh`.

## Validação de 27/10/2026

Seis LG 43LM631C0SB, firmware 05.40.97 e SDK 4.10.0. Todas receberam o mesmo pacote e abriram o OwnTV. Em cada unidade, 16 verificações automatizadas passaram no motor real da TV, sem erros de JavaScript observados: Home, recursos, limites dos botões, navegação vertical, configurações, persistência, quatro controles de entrada preenchimento translúcido e camada de abertura acima dos cards. Foram capturadas as seis telas reais.

Os comandos HDMI 1, 2, 3 e Central de entradas foram conferidos separadamente. Aceitação de um comando pelo sistema não certifica sinal, áudio, resolução ou HDCP de um equipamento HDMI. Os testes de navegação usam eventos sintetizados pelo depurador; a operação completa com controle físico ainda precisa de conferência humana.

Nenhuma credencial, endereço real, foto de Developer Mode, configuração de câmera ou log privado integra esta entrega. A política de recursos locais bloqueia scripts remotos, frames e objetos.

## Limites

É um aplicativo de desenvolvimento; Home continua sendo LG. Sem root, SSH permanente, câmeras, notícias ou recomendações nesta versão. Developer Mode pode expirar e remover aplicativos. Não inclui navegador nem abertura automática de sites. Veja a lista completa no guia do piloto.
