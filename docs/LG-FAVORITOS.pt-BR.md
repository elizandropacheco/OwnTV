# Aplicativos e favoritos nas LG

Disponível no **OwnTV webOS 0.2.1**, separado do APK Android. A seleção e a ordem são próprias de cada TV.

![Seleção de favoritos na LG](screenshots/webos-favorites.png)

## Escolher e ordenar

1. Na página Início, escolha **Editar favoritos**. Também é possível usar **Apps → Escolher favoritos**, o rodapé **Personalizar apps** ou **Configurações → Aplicativos favoritos**.
2. A lista mostra os aplicativos fornecidos pela própria LG, com seus nomes e ícones coloridos. **OK** adiciona ou remove um favorito. A estrela indica os selecionados.
3. Em **Ordenar favoritos**, escolha um item e pressione **OK**. Use **←/→** para mover uma posição e **↑/↓** para mover três posições. **OK** conclui; **Voltar** solta o item na posição atual.
4. As escolhas são salvas automaticamente. Remover um favorito não desinstala o aplicativo. É permitido deixar a lista vazia; o início mostrará **Adicionar favoritos**.
5. **Todos os apps** abre os aplicativos com OK. A lista usa nove cards completos por página, com botões Anteriores/Próximos; as setas também atravessam as páginas.

Na página Início aparecem **quatro favoritos completos por página**. ←/→ percorrem a ordem e avançam para o próximo grupo quando necessário; os botões de paginação também funcionam. ↓ a partir do menu superior sempre começa no primeiro favorito. ↑ a partir dos favoritos vai a Câmeras, preservando a navegação já utilizada.

A atualização de versões antigas preserva os quatro atalhos e sua ordem. Outros aplicativos são escolhidos pelo usuário; descobrir um app não o adiciona automaticamente aos favoritos. TV ao vivo, Entradas e Personalizar continuam disponíveis como atalhos adicionais.

## Pareamento com a própria TV

A API pública Luna bloqueia a listagem geral no firmware testado. O OwnTV consulta a lista pelo protocolo de controle remoto **SSAP**, conectando-se ao endereço de rede da própria TV. Isso não depende da ponte das câmeras, de root ou de um Mac permanentemente ligado.

1. Mantenha a TV conectada à rede local e o [Developer Mode ativo](LG-WEBOS.pt-BR.md).
2. Em Apps ou Editar favoritos, escolha **Atualizar apps**.
3. Quando a LG pedir autorização para uma conexão de controle remoto, escolha **Permitir**. O pedido pode levar alguns segundos; o OwnTV aguarda até três minutos.
4. A lista será carregada e poderá ser atualizada novamente após instalar/remover aplicativos na LG.

Nas aberturas seguintes o OwnTV reutiliza o pareamento e tenta atualizar a lista. Sem chave, não solicita autorização automaticamente. Se a conexão falhar, mantém o catálogo e as preferências anteriores, com uma mensagem explicando o problema. Se um app desaparecer de uma consulta concluída com sucesso, ele sai dos favoritos. A lista é fornecida pela LG: atalhos que ela não expõe não são inventados pelo OwnTV.

O mecanismo foi validado nas seis LG 43LM631C0SB, firmware 05.40.97/SDK 4.10.0, usando o WebSocket local na porta 3000. Não estabelece compatibilidade com modelos que só aceitem outro protocolo/porta.

## Dados e segurança

- A chave fica no arquivo privado `private-data/apps-pairing.json` do serviço OwnTV, com permissão 0600 e diretório 0700. Não é colocada no IPK, no GitHub ou no armazenamento de preferências da interface.
- Somente o próprio app pode ler ou gravar essa conexão pelo serviço. A interface não aceita um endereço remoto arbitrário: o serviço fornece os endereços das interfaces da própria TV.
- Nomes, IDs, URLs dos ícones e a ordem ficam localmente na TV. O projeto publicado não inclui inventários pessoais nem endereços reais dos aparelhos.
- Os ícones vêm da TV, em sua resolução original; não são baixados de sites de terceiros. Se um ícone falhar, o card usa as iniciais do nome.
- O acesso às câmeras continua em sua configuração privada separada.

## Home e abertura dos aplicativos

**O botão físico Home continua abrindo a interface LG.** Esta entrega implementa favoritos dentro do OwnTV; não instala um interceptador de teclas.

O teste de `com.webos.applicationManager/setDefaultApp` respondeu **Unknown method** nas seis TVs. Portanto, esse método de troca direta da Home não é utilizável no firmware testado. Existem [projetos com auxiliares nativos sem root](https://github.com/Razdva122/clean-your-tv-lg-webos/blob/main/docs/COMPATIBILITY.md), mas a compatibilidade de entrada e recuperação precisa de validação própria; nenhum auxiliar foi instalado por esta entrega. A falha de uma API não demonstra impossibilidade de todos os outros métodos.

Os favoritos solicitam a abertura pelo ID real do aplicativo. A LG pode exigir atualização ou redirecionar um app à Content Store. No teste do Netflix, quatro aparelhos abriram Netflix e dois abriram a loja LG; não foi confirmado o motivo desse redirecionamento. O resultado não é apresentado como sucesso do Netflix nas seis. O YouTube abriu nas seis pelo card do OwnTV, confirmado pelo aplicativo em primeiro plano.

Veja o [registro da versão](../releases/webos/0.2.1/README.md) para os testes e limites da evidência.
