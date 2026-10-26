# Contribuir com OwnTV

OwnTV é desenvolvido por Elizandro Pacheco com base no Couchy Launcher, de Conreo. Contribuições de código seguem GPLv3, preservando licença e créditos. Veja [LICENSING.md](LICENSING.md) para componentes e recursos.

## Preparar o projeto

Siga [Compilação, assinatura e publicação](docs/BUILDING.md) e o [mapa do código](docs/PROJECT_STRUCTURE.md). Android mínimo: API 26; pacote instalado: `io.elizandropacheco.tv`.

```sh
git clone https://github.com/elizandropacheco/OwnTV.git
cd OwnTV
./gradlew :app:assembleDebug
```

Após a limpeza do histórico dos logos, use um clone novo. Não mescle branches anteriores, pois elas podem reintroduzir os arquivos removidos.

## Alterações

- Mantenha cada mudança focada em um comportamento observável e explique o motivo.
- Garanta acesso por D-pad e confira o foco ao entrar, voltar e percorrer páginas.
- Preserve preferências existentes e defina padrões/migrações para novos campos.
- Evite rede ou decodificação pesada na thread da interface; pause tarefas visuais fora da tela.
- Use recursos em `res/values/` para novas mensagens traduzíveis.
- Preserve permissões normais do Android, reversibilidade e acesso às configurações do aparelho.
- Não inclua originais editáveis de logos, chaves, credenciais, endereços privados, backups ou imagens de câmeras reais.

O namespace `com.conreo.couchytv` preserva a base e difere do `applicationId` OwnTV. As receitas F-Droid em `docs/upstream/` são referência histórica Couchy, não uma publicação atual do OwnTV.

## Validação

```sh
python3 scripts/check-public-assets.py
./scripts/build-release.sh
```

O build gera APK unsigned sem assinatura configurada. Para instalar, use uma chave compatível conforme o guia. Não desinstale a versão em uso para contornar assinatura diferente sem tratar backup e perda de dados.

Teste no aparelho os comportamentos alterados. Mudanças visuais precisam de conferência em 16:9, foco visível, ausência de cortes e tela estável na seleção. Imagens de demonstração não devem conter dados pessoais nem feeds reais de câmeras.

Na contribuição, descreva problema, mudança, testes e limitações. Não declare CI remota ou testes no aparelho que não ocorreram.

## Releases

Versão e código ficam em `app/build.gradle.kts`; mudanças em `CHANGELOG.md`. O mantenedor valida e publica APK, SHA-256 e metadados separados na GitHub Release, mantendo código e documentação navegáveis. Siga o [fluxo de publicação](docs/BUILDING.md#publicar-uma-versão).
