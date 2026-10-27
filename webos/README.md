# OwnTV para LG webOS — 0.2.0 experimental

Implementação separada do Android, com interface HTML/CSS/JavaScript para o Chromium 53 e serviço JavaScript compatível com Node 0.12.2 das LG de referência. O APK Android não roda em webOS.

![Home OwnTV webOS com notícias e sugestões](../docs/screenshots/webos-home.png)

**Home físico ainda abre a LG. A lista completa para escolher apps favoritos não está implementada.** Consulte o [estado atual e as pendências](../docs/STATUS.pt-BR.md).

## Recursos

- Home aberta, identidade EP/OwnTV configurável, abertura animada, transparência e contornos em gradiente. Foco sem ampliar ou deslocar cards.
- Quinze fundos da coleção Android em Full HD, alternância inicial de um minuto e escurecimento configurável.
- Bloco Favoritos com quatro atalhos predefinidos: YouTube, TV ao vivo, Entradas e Personalizar. Cores/monocromático e ordem persistente; ainda não permite escolher outros apps instalados.
- Notícias reais em duas linhas abaixo do slogan, alternando a cada 12 segundos; página de notícias com fonte, tema, data e paginação. Temas iniciais: telecom, tecnologia e inteligência artificial. Configuração aceita até oito temas e permite desligar o recurso.
- Sugestões do YouTube: quatro cards completos por página, miniatura, título, canal e data, na Home e na página Sugeridos. Os canais públicos são configuráveis. Os padrões incluem Elizandro Pacheco, Canaltech, TecMundo e OpenAI.
- Câmeras: seis prévias por página em três colunas, navegação entre canais, vídeo principal ampliado, modo leve, pausa, áudio, preenchimento/ajuste e reconexão. Requer a [ponte privada](bridge/README.md) em um computador/servidor ligado.
- Configurações em três colunas, com controles próprios para notícias, canais e conexão das câmeras.
- Entradas HDMI 1–3 e Central de entradas LG; atalhos para YouTube e TV ao vivo.

## Usar pelo controle

**↓ no menu superior** vai ao primeiro favorito. **↑ nos favoritos** vai a Câmeras, depois Jogos e menu. **↓ nos favoritos** vai à sugestão da mesma coluna. As setas nas sugestões avançam/voltam a página. Voltar fecha o vídeo e restaura a câmera selecionada; outro Voltar retorna à Home. O player usa uma camada própria, fora da transformação de escala da interface, para funcionar com a saída de vídeo das LG.

Em **Configurações → Notícias e temas**, edite temas separados por vírgula. Em **Sugestões do YouTube**, informe IDs públicos de canais (`UC…`, 24 caracteres), separados por vírgulas. Não informe URLs de login, cookies ou senhas. Os campos aceitam o teclado virtual da TV.

RSS é atualizado com cache de 30 minutos e cópia local para indisponibilidade temporária. A interface identifica conteúdo em cache ou falha de atualização; não inventa notícias. Notícias são manchetes, não artigos completos. Consultas dos temas são enviadas ao Google Notícias; IDs públicos são enviados ao YouTube. Miniaturas vêm do YouTube.

As sugestões são vídeos públicos dos canais escolhidos, **não** recomendações pessoais, histórico ou inscrições da conta. O RSS não fornece duração; o app não inventa esse dado. Abrir um card solicita o vídeo ao aplicativo YouTube instalado, cujo comportamento depende da versão. Outros serviços não fornecem aqui o provedor de conteúdo do Android TV.

## Instalar e atualizar

Primeiro siga o [guia de conta LG, país, Developer Mode, chaves e conexão](../docs/LG-WEBOS.pt-BR.md). Baixe o IPK da [release webos-v0.2.0](https://github.com/elizandropacheco/OwnTV/releases/tag/webos-v0.2.0), confira SHA256SUMS e instale:

```sh
ares-install --device lg-piloto io.elizandropacheco.owntv.webos_0.2.0_all.ipk
ares-launch --device lg-piloto io.elizandropacheco.owntv.webos
```

Repita com o nome cadastrado de cada TV. Atualize por cima, sem desinstalar ou limpar os dados. O IPK inclui app e serviço de conteúdo. As câmeras são configuradas separadamente e nenhum acesso real acompanha o pacote. Mantenha Developer Mode ativo e acompanhe sua validade; desativação/expiração pode remover apps de desenvolvimento.

## Compilar a partir do código

Requisitos: Node.js compatível com a CLI oficial LG, npm, Python 3 e Pillow para preparar os fundos.

```sh
npm install -g @webos-tools/cli
npm ci --prefix webos
python3 -m pip install Pillow
python3 scripts/fetch-wallpapers.py --count 15
python3 scripts/prepare-webos-assets.py
./scripts/build-webos.sh
```

Para uma CLI instalada em outro caminho, use `OWNTV_ARES_PACKAGE=/caminho/ares-package`. O build executa verificações de arquivos públicos, sintaxe e testes de dados/preferências, empacota `webos/app/` e `webos/service/` e grava IPK, SHA256SUMS e build-info em `releases/webos/0.2.0/`. O binário é distribuído pela GitHub Release; o fonte permanece navegável, sem ZIP obrigatório.

Verificações adicionais:

```sh
npm test --prefix webos
node webos/tests/content.test.cjs
node webos/tests/model.test.cjs
python3 webos/tests/bridge_test.py
python3 scripts/check-public-assets.py
```

## Limites atuais

- App aberto pelo menu LG. Não substitui Home, não inicia automaticamente ao ligar a TV e não fornece root/SSH permanente.
- SSH Developer Mode continua sendo `prisoner`, com prazo e permissões LG. Não foi criado usuário de sistema adicional.
- Lista de apps contém os atalhos conhecidos, não um inventário geral: as APIs de inventário foram negadas nesse firmware. Música não integra catálogo próprio; jogos usam atalhos HDMI.
- Câmeras dependem de servidor externo. As prévias são JPEGs atualizados a cada três segundos; não são seis vídeos simultâneos. Um vídeo ao vivo por TV, com atraso HLS. Veja [instalação, privacidade e limites da ponte](bridge/README.md).
- Navegador/URL inicial ficaram fora do escopo. Imagens personalizadas, download diário de fundos, integrações IoT/Alexa e funções exclusivas TCL não foram portados.
- Testado em seis LG 43LM631C0SB, firmware 05.40.97, SDK 4.10.0. Não é homologação para todos os modelos LG.

Veja [validação e ressalvas](../docs/VALIDATION.md), [histórico](../CHANGELOG.md) e [hardware](../docs/LG-WEBOS.pt-BR.md#5-hardware-verificado).

Desenvolvido por [Elizandro Pacheco](https://elizandropacheco.io/). [Siga no Instagram](https://www.instagram.com/elizandropacheco/), [inscreva-se no YouTube](https://www.youtube.com/@ElizandroPacheco) e [dê uma estrela no GitHub](https://github.com/elizandropacheco/OwnTV).
