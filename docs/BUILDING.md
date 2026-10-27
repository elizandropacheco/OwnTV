# Compilar, verificar e publicar OwnTV

O repositório contém o código Android, Gradle Wrapper, recursos de execução, scripts e documentação. Uma cópia obtida por `git clone` pode ser aberta no Android Studio ou compilada pelo terminal, sem um ZIP de código ou arquivos privados do desenvolvedor.

## Requisitos

- Git e Python 3.9 ou posterior; os scripts de entrega usam somente a biblioteca padrão.
- JDK 21 (ambiente validado com Gradle 8.9; o bytecode Android continua com alvo Java 17).
- Android SDK: plataforma `android-34`, Build Tools `35.0.0` e `platform-tools` para ADB.
- Internet para o primeiro download do Gradle e das dependências Maven.

O projeto usa Android Gradle Plugin 8.5.2, Kotlin 2.4.21 e R8 9.1.56 no classpath de build. Android mínimo: API 26; `targetSdk`: 34. O pacote instalado é **`io.elizandropacheco.tv`**; o namespace **`com.conreo.couchytv`** preserva a base do projeto.

Kotlin 2.4.21 corrige o aviso de desserialização de metadados do cache de build. A [matriz oficial Kotlin](https://kotlinlang.org/docs/gradle-configure-project.html) aceita Gradle 8.9 e AGP 8.5.2 nessa versão. A [matriz Android](https://developer.android.com/build/kotlin-support) exige R8 9.1.29 ou posterior para Kotlin 2.4; o projeto fixa a versão estável 9.1.56, publicada no Maven do Google. O analisador Lint é fixado em `android.experimental.lint.version=9.4.1` (artefatos 32.4.1), pois o analisador original do AGP não compreende os metadados Kotlin 2.4. Esse override é [documentado pelo Android](https://googlesamples.github.io/android-custom-lint-rules/usage/newer-lint.md.html); nenhuma regra é suprimida para contornar incompatibilidade. O bloco `buildscript` também fixa correções de dependências transitivas das ferramentas. Esses ajustes não substituem validação completa de build, lint e execução na TV.

Regras de metadados restritas às dependências do lint corrigem Commons Lang, HttpClient e Bouncy Castle também no classpath separado do analisador. Os JARs efetivamente carregados entram na auditoria de dependências. A propriedade de override tem nome experimental, mas a versão escolhida do analisador é estável.

Instale o SDK pelo SDK Manager do Android Studio ou pelas Command-line Tools. Com `sdkmanager` disponível:

```sh
sdkmanager 'platform-tools' 'platforms;android-34' 'build-tools;35.0.0'
sdkmanager --licenses
```

Configure `JAVA_HOME` para o JDK e `ANDROID_HOME` para seu SDK. Exemplo Linux, substituindo o caminho do JDK:

```sh
export JAVA_HOME=/caminho/para/jdk-21
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
```

No macOS, o SDK do Android Studio costuma ficar em `$HOME/Library/Android/sdk`. Como alternativa a `ANDROID_HOME`, crie `local.properties` com `sdk.dir=/caminho/para/Android/sdk`. Esse arquivo é local e não deve ser commitado.

## Obter o código

```sh
git clone https://github.com/elizandropacheco/OwnTV.git
cd OwnTV
```

Para compilar uma versão publicada, use sua tag:

```sh
git switch --detach v0.2.4
```

Após a limpeza do histórico de logos, substitua clones antigos por um clone novo. Não faça merge de branches anteriores, pois isso pode republicar os arquivos removidos. Preserve mudanças próprias fora da árvore e reaplique somente as alterações de código necessárias.

## Recursos e imagens

O código inclui imagens renderizadas para a identidade visual funcionar. Os originais editáveis de logos ficam fora da distribuição e não são necessários para compilar. Fotografias grandes são baixadas das fontes creditadas, opcionalmente:

```sh
python3 scripts/fetch-wallpapers.py --count 15
```

O script preserva pixels, resolução, orientação e perfil de cor, retirando metadados de captura/localização sem recompressão. Sem fotografias, o aplicativo mantém o fundo de fallback. Catálogo e créditos ficam em `app/src/main/assets/ep/wallpapers/`. `build-info.json` registra a quantidade de fotos incluídas e a presença de arte opcional de jogo. Recursos locais, assinatura e ambiente podem alterar o hash; não se promete reprodução idêntica byte a byte.

## Desenvolvimento

```sh
./gradlew :app:assembleDebug
```

O APK fica em `app/build/outputs/apk/debug/app-debug.apk`. No Windows, use `gradlew.bat`. Para medir fluidez, prefira release: Compose debug tem instrumentação adicional. Instale versões de desenvolvimento somente quando a assinatura for compatível com a instalação existente.

## Build de entrega em releases/

```sh
./scripts/build-release.sh
```

No Windows:

```powershell
python scripts/package-release.py
```

O comando valida arquivos não publicáveis, executa `:app:assembleRelease` e `:app:lintRelease`, confere pacote/versão/ausência de `debuggable`, verifica a assinatura com `apksigner` e gera APK, `SHA256SUMS.txt` e `build-info.json` em `releases/<versionName>/`.

A versão vem de `app/build.gradle.kts`. O manifesto registra commit de origem, alterações locais, horários UTC, SDK, validações, tamanho, hash e digest público do certificado. Não inclui caminhos locais nem material privado da chave.

```sh
./scripts/build-release.sh --help
./scripts/build-release.sh --clean
./scripts/build-release.sh --offline
```

`--clean` reconstrói os outputs. `--offline` exige SDK e dependências já disponíveis. O script não instala o SDK e não oferece `--skip-build`. Uma falha de compilação, lint, manifesto ou assinatura não substitui a entrega anterior.

## Assinatura

### Sem chave

O arquivo recebe `-unsigned.apk` e precisa ser assinado antes de instalar.

### Chave de distribuição própria

Guarde a chave fora do repositório. Exemplo em **Bash**, sem senhas literais no histórico:

```sh
export KEYSTORE_FILE=/caminho/absoluto/privado/owntv-release.jks
export KEY_ALIAS=owntv
read -r -s -p 'Senha do keystore: ' KEYSTORE_PASSWORD
printf '\n'
export KEYSTORE_PASSWORD
read -r -s -p 'Senha da chave: ' KEY_PASSWORD
printf '\n'
export KEY_PASSWORD
./scripts/build-release.sh
unset KEYSTORE_FILE KEYSTORE_PASSWORD KEY_ALIAS KEY_PASSWORD
```

As quatro variáveis são obrigatórias em conjunto. `KEYSTORE_FILE` deve ser um caminho absoluto existente. Para uma nova distribuição, crie a chave pelo assistente **Generate Signed Bundle / APK** do Android Studio ou com `keytool` e mantenha um backup privado. Uma chave nova não é compatível com uma instalação assinada com outra chave.

### Compatibilidade com a instalação local inicial

```sh
./scripts/build-release.sh --local-sign
```

Essa opção exige a **chave debug local já existente** e usa `-PlocalSign`. Serve para desenvolvimento ou compatibilidade com a instalação inicial. O APK continua otimizado e não depurável; a chave debug não se torna uma chave de produção. Outra máquina normalmente terá outra chave debug.

O script rejeita a combinação de `--local-sign` com chave externa. Confira o digest do certificado antes de atualizar:

```sh
"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --print-certs releases/0.2.4/OwnTV-0.2.4.apk
```

Não desinstale nem limpe dados para contornar `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: isso apaga configurações e pode inviabilizar dados protegidos pelo Android Keystore. Recupere a chave original.

## Instalar e validar na TV

O [guia de instalação](INSTALLATION.pt-BR.md) explica modo desenvolvedor, depuração, autorização ADB, Home/acessibilidade, câmeras, SSH e reversão. Com ADB autorizado e assinatura compatível:

```sh
adb connect TV_IP:PORTA_ADB
adb install -r releases/0.2.4/OwnTV-0.2.4.apk
adb shell am start -n io.elizandropacheco.tv/com.conreo.couchytv.MainActivity
```

Valide D-pad, Home físico, logo/abertura, favoritos, recomendações e páginas afetadas. Câmeras e SSH precisam de teste autorizado na rede alvo; nunca use credenciais publicadas. Build/lint não substituem teste no aparelho. Veja [Validação e limites](VALIDATION.md).

## Publicar uma versão

1. Atualize `versionName`, incremente `versionCode` e registre mudanças em `CHANGELOG.md`.
2. Verifique a árvore pública e faça commit do código. O build publicado deve registrar `sourceDirty: false`.
3. Compile com a chave compatível e valide no aparelho.
4. Versione metadados de `releases/<versão>/`; APKs permanecem fora do Git. Confira `git diff --cached`.
5. Crie a tag, envie código/tag e publique APK, checksum e manifesto separados.

Com código/metadados já commitados e GitHub CLI autenticado:

```sh
git tag -a v0.2.4 -m 'OwnTV 0.2.4'
git push origin main
git push origin v0.2.4
gh release create v0.2.4 \
  releases/0.2.4/OwnTV-0.2.4.apk \
  releases/0.2.4/SHA256SUMS.txt \
  releases/0.2.4/build-info.json \
  --verify-tag --title 'OwnTV 0.2.4' --notes-file releases/0.2.4/RELEASE_NOTES.md
```

Prepare `RELEASE_NOTES.md` com mudanças e validações reais antes do comando. Não republique uma tag com outro binário: gere nova versão. ZIPs/tarballs de código oferecidos automaticamente pelo GitHub não substituem a árvore aberta do repositório e não são o formato de entrega do projeto.

## Automação e diagnóstico

`docs/ci/build.workflow.yml` é um **modelo**, não CI ativa. O mantenedor pode revisá-lo e colocá-lo em `.github/workflows/build.yml` usando credencial com permissão de workflows. Assinatura/publicação automática requer configuração explícita de secrets e permissões; não há badge de CI remoto aprovado sem execução real.

| Falha | Conferência |
| --- | --- |
| SDK ou Build Tools não encontrados | Configure `ANDROID_HOME` ou `local.properties`; instale API 34/Build Tools 35.0.0 |
| Gradle não inicia | JDK, `JAVA_HOME` e execução de `gradlew` |
| Dependência não baixa | Rede, proxy, Maven e cache; `--offline` não baixa arquivos |
| Arquivo privado encontrado | Mova-o para fora da árvore pública e revise arquivos rastreados |
| APK unsigned não instala | Configure assinatura antes do build |
| Assinatura incompatível | Recupere a chave original, sem apagar os dados |
| `sourceDirty: true` | Faça commit do código antes de gerar a versão pública |
| Sem paisagens offline | Execute o download opcional antes de compilar |

## LG webOS 0.2.0

Veja [o guia de build webOS](../webos/README.md#compilar-a-partir-do-código). `scripts/build-webos.sh` empacota app e serviço ES5 em um único IPK. A ponte Python é executada separadamente, fora da TV. Sua configuração privada e os diretórios `private-data` do serviço nunca entram no pacote; o verificador público bloqueia essa situação. Use Node 0.12.2/Chromium 53 como limites de sintaxe para o firmware LG de referência, independentemente da versão usada para empacotar no computador.
