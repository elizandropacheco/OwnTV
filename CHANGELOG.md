# Alterações

## Não lançado

- Documentação LG: conta Developer, erro de região Brasil/EUA, ativação, CLI/SSH, expiração e diagnóstico.
- Hardware e acesso de desenvolvedor verificados em seis LG 43LM631C0SB; root, Home e port webOS permanecem não validados.
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
