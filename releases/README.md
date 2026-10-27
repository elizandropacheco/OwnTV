# Releases do OwnTV

Esta pasta é o ponto de entrega dos builds e de seus metadados. O código permanece aberto e navegável em `app/`, `scripts/` e `docs/`; não é entregue dentro de um ZIP.

## Baixar a versão publicada

**[OwnTV 0.2.4](https://github.com/elizandropacheco/OwnTV/releases/tag/v0.2.4)**

- [APK](https://github.com/elizandropacheco/OwnTV/releases/download/v0.2.4/OwnTV-0.2.4.apk)
- [SHA256SUMS.txt](https://github.com/elizandropacheco/OwnTV/releases/download/v0.2.4/SHA256SUMS.txt)
- [build-info.json](https://github.com/elizandropacheco/OwnTV/releases/download/v0.2.4/build-info.json)

Os APKs ficam nos anexos de GitHub Releases, sem aumentar o histórico Git a cada versão. Na máquina de desenvolvimento, o mesmo fluxo os coloca diretamente nesta pasta:

```text
releases/
├── README.md
└── 0.2.4/
    ├── OwnTV-0.2.4.apk        # gerado localmente; download na release
    ├── SHA256SUMS.txt        # integridade do APK daquela compilação
    └── build-info.json       # origem, versão, assinatura e verificações
```

## Gerar uma entrega

```sh
./scripts/build-release.sh
```

Sem chave, o arquivo se chama `OwnTV-0.2.4-unsigned.apk` e não pode ser instalado. Para assinar, siga [Compilação e assinatura](../docs/BUILDING.md). O script verifica os arquivos públicos, executa build e lint, confere manifesto/assinatura e só então atualiza a entrega. Não há opção de pular a compilação e reaproveitar um APK sem verificar sua origem.

`build-info.json` distingue assinatura externa, chave debug local e APK unsigned. Registra `sourceCommit` e `sourceDirty`, sem caminhos da máquina ou senhas. `sourceCommit` identifica o código usado no build; um commit posterior pode apenas acrescentar os metadados.

## Conferir o download

Baixe APK e `SHA256SUMS.txt` para a mesma pasta. No macOS:

```sh
shasum -a 256 -c SHA256SUMS.txt
```

No Linux:

```sh
sha256sum -c SHA256SUMS.txt
```

No PowerShell, compare o resultado com `SHA256SUMS.txt`:

```powershell
Get-FileHash .\OwnTV-0.2.4.apk -Algorithm SHA256
```

Use o hash daquela release: assinaturas, recursos opcionais e ambientes diferentes podem produzir bytes diferentes. Veja [Instalação e atualização](../docs/INSTALLATION.pt-BR.md) para preservar os dados e a assinatura.

## Publicação

O script não faz push, não cria tags e não publica por conta própria. O [fluxo do mantenedor](../docs/BUILDING.md#publicar-uma-versão) envia APK, checksum e manifesto separados.

Não são distribuídos originais editáveis de logos, chaves, backups nem configurações reais de câmeras ou SSH. A entrega 0.2.2 foi substituída na distribuição pública pela versão sem esses originais.

## LG webOS — entrega separada

A [versão experimental 0.2.1](webos/0.2.1/README.md) usa IPK, não APK. Código em `webos/app/`, `webos/service/` e `webos/bridge/`, script `scripts/build-webos.sh` e saída em `releases/webos/<versão>/`. O IPK é anexado à [pré-release webos-v0.2.1](https://github.com/elizandropacheco/OwnTV/releases/tag/webos-v0.2.1), acompanhado de SHA256SUMS e build-info.json. A versão Android permanece a release principal.
