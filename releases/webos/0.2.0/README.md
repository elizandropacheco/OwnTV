# OwnTV webOS 0.2.0 — experimental

Esta versão acrescenta notícias, sugestões públicas do YouTube e câmeras ao port LG, mantendo Android em 0.2.4.

- Manchetes por temas, fonte/data e página de notícias; configuração para até oito temas.
- Quatro sugestões completas por página, canal e data, com canais públicos configuráveis.
- Mosaico de **três colunas e seis câmeras por página**, preservando proporções. Vídeo principal e modo leve, preencher/ajustar, pausa, áudio, reconexão e retorno ao canal selecionado.
- Correção do vídeo preto: player fora da camada transformada da interface. Enquadramento do vídeo feito na ponte, sem depender de `object-fit` do player LG.
- Preferências e chaves da ponte preservadas durante a atualização sobre a instalação anterior.
- Serviço ES5, testes de compatibilidade, isolamento de chamadas e segurança das rotas da ponte.

## Instalação

Leia [conta LG e Developer Mode](../../../docs/LG-WEBOS.pt-BR.md), [instalação do IPK](../../../webos/README.md) e [ponte de câmeras](../../../webos/bridge/README.md). Baixe o IPK e SHA256SUMS da [release](https://github.com/elizandropacheco/OwnTV/releases/tag/webos-v0.2.0), verifique o hash e atualize por cima, sem desinstalar.

O código está nas pastas `webos/app`, `webos/service` e `webos/bridge`; não depende de um ZIP de fontes. O script `scripts/build-webos.sh` gera o IPK nesta pasta. Arquivos binários são anexos da GitHub Release, não versões duplicadas no histórico Git.

## Validação e limites

Veja [validation.json](validation.json) e [o registro completo](../../../docs/VALIDATION.md). Testado em seis LG 43LM631C0SB, firmware 05.40.97/SDK 4.10.0. Imagens, IPs e configurações reais de câmeras não acompanham o registro público.

A ponte externa precisa permanecer ligada. A execução iniciada no Mac foi testada; o LaunchAgent encontrou restrição de rede local e foi desativado. O instalador automático é fornecido, mas precisa de autorização/validação no computador de destino. Não há promessa de reinício automático já funcionando nesta instalação.

As sugestões não usam histórico nem recomendações pessoais da conta YouTube. O webOS não fornece ao OwnTV o mesmo provedor de conteúdo dos aplicativos Android. Home LG, root, SSH permanente, usuário SSH extra e navegador continuam fora desta versão.
