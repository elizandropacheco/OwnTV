# Identidade visual e personalização

![OwnTV](branding/owntv-wordmark.png)

No Android, o logo EP de Elizandro Pacheco continua como padrão na Home, nos menus, na abertura, no ícone e no banner. Em **Configurações → Personalizar sua central → Identidade visual e logo**, escolha EP, OwnTV ou uma imagem PNG/JPEG/WebP local ou USB. A opção **Pré-visualizar abertura** mostra a animação.

## Arquivos publicados

A árvore atual do repositório e os builds APK/IPK usam somente imagens PNG renderizadas para os logos próprios. Não são distribuídos os originais editáveis, arquivos SVG, matrizes vetoriais Android, desenhos em código ou pacotes de identidade visual. Os PNGs do aplicativo têm até 1024 pixels por dimensão; o banner e a assinatura da documentação usam até 1280 pixels. São derivados para exibição, não arquivos de criação.

Os originais ficam fora da árvore e dos builds atuais. **A remoção de objetos históricos acessíveis por hash no GitHub ainda não foi concluída**; não confunda a revisão dos arquivos atuais com a eliminação dessas cópias antigas. Veja [o estado da pendência](STATUS.pt-BR.md). Não são necessários para compilar. A animação usa opacidade e escala dos PNGs e preserva a proporção; aparece na primeira abertura do processo, sem se repetir a cada Home. A primeira imagem de boot do firmware da TV não é alterada.

## Personalização

Use uma imagem própria, preferencialmente PNG transparente, pelo seletor do Android. A imagem fica na TV e não é enviada ao GitHub. Uma mídia USB removida pode deixar a imagem indisponível; o aplicativo usa o logo EP como alternativa.

Para um fork, substitua os PNGs em `app/src/main/res/drawable-nodpi/` por sua identidade, mantendo os nomes de recursos ou atualizando as referências. A GPLv3 do código não transfere titularidade de marcas nem os arquivos de criação privados. Veja [LICENSING.md](../LICENSING.md).

Antes de publicar, execute `python3 scripts/check-public-assets.py`; o script de build também executa essa verificação.

## Diferença no webOS

O port LG oferece EP e monograma OwnTV. Importar uma imagem própria para Home, menus e abertura ainda não foi implementado; o seletor Android descrito acima não está disponível nas LG.
