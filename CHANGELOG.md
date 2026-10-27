# Alterações

## webOS 0.2.1 — 2026-10-27

- Catálogo real da LG por pareamento local, com chave isolada no serviço privado.
- Seleção e remoção de favoritos, ordenação por controle, ícones nativos e lista vazia suportada.
- Nove apps por página e quatro favoritos por grupo na Home, com foco e paginação.
- Migração das preferências antigas e documentação de pareamento e segurança.
- Home físico permanece LG; a API de launcher padrão não existe no firmware das seis TVs testadas.


## Documentação — 27/10/2026

- Estado consolidado, matriz Android/LG, índice dos guias e critérios de conclusão das pendências.
- Home físico LG e ausência de seleção completa de favoritos destacados nos READMEs.
- Registro complementar da reprodução nas seis TVs e distinção entre recursos atuais e limpeza histórica dos logos.
- Sem alteração dos binários Android 0.2.4/webOS 0.2.0.

## webOS 0.2.0 — 27/10/2026 (experimental)

- Notícias RSS por temas com manchetes rotativas, fonte, data, paginação e cache.
- Sugestões públicas de canais YouTube configuráveis: quatro cards, metadados compactos e abertura no app YouTube.
- Câmeras com ponte privada autenticada, seis prévias JPEG por página, vídeo HLS principal/leve e controles próprios.
- Player fora da transformação da interface, evitando vídeo preto na saída nativa LG; gradientes ajustados à altura dos cards.
- Configurações de conteúdo em três colunas, navegação entre favoritos e sugestões e tratamento de indisponibilidade.
- Serviço ES5 incluído no IPK, limites de rede/cache, testes de parsers e autorização da ponte, guias de instalação e privacidade.
- Android permanece na versão 0.2.4. Home LG, root e SSH permanente não fazem parte desta versão.


## 0.2.4 Android / 0.1.1 webOS — 2026-10-27

- Android: botão Entradas no rodapé abre o seletor existente da TV.
- webOS: rotação automática de fundos a cada minuto e abertura com logo acima de todas as camadas.
- webOS: transparência real dos cards, bordas independentes e seletor HDMI 1–3/Central LG acessível na Home.
- Piloto webOS 0.1.1: composição baseada no Android, quinze fundos Full HD, menus, cards, foco estável, cores, identidade e ordenação de favoritos.
- Build IPK separado e testes de preferências; corrigido carregamento de recursos locais no Chromium 53.
- Navegador e opções de URL retirados do escopo a pedido do usuário; câmeras, notícias, sugestões e substituição da Home permanecem pendentes no webOS.

- Documentação LG: conta Developer, erro de região Brasil/EUA, ativação, CLI/SSH, expiração e diagnóstico.
- Hardware, instalação do piloto e navegação verificados nas seis LG 43LM631C0SB; root, Home e SSH permanente não foram implementados.
- README em português/inglês atualizado e manutenção da documentação incluída no fluxo de contribuição.

## 0.2.3

- Código em árvore aberta, com mapa de responsabilidades e instruções completas de build, assinatura e publicação.
- Scripts que compilam, executam lint, conferem manifesto/assinatura e entregam APK, SHA-256 e metadados em `releases/<versão>/`.
- Originais editáveis de logos retirados da distribuição; identidade visual mantida com imagens renderizadas.
- Histórico público reiniciado para não conservar commits que distribuíam originais; a antiga entrega 0.2.2 é substituída.
- Orientações de contribuição corrigidas para pacote, SDK mínimo e releases do OwnTV.

## 0.2.2

- Favoritos coloridos por padrão, estilo monocromático opcional e ordem configurável.
- Navegação: menu → favoritos; favoritos → Câmeras → PS5 → menu ao subir.
- Recomendações reais do YouTube em primeiro, quatro sugestões completas e metadados compactos.
- Identidade configurável, abertura animada e prévia nas configurações.
- Notícias em duas linhas, foco revisado e câmeras com preenchimento padrão.
- Home físico verificado na TCL Android TV 14; release, lint e SSH/SFTP validados na instalação de referência.

## 0.2.1

- Base OwnTV sobre Couchy Launcher, com Home aberta, personalização, jogos/HDMI, câmeras, wallpapers e SSH/SFTP.

As entradas históricas documentam a evolução sem preservar os originais ou commits removidos.

### Revisão de segurança da entrega

- HTTP claro bloqueado; validação TLS preservada e comentários herdados corrigidos.
- Limites de autenticação, conexões, sessões e canais SSH; peers públicos rejeitados.
- Downloads de fotos limitados, HTTPS, gravação atômica e remoção de metadados de captura/localização sem recompressão.
- Wrapper Gradle oficial e checksum da distribuição fixado.
- Configurações reais de câmeras, credenciais, backups e capturas privadas excluídos da distribuição.
