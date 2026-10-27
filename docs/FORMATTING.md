# Formatação do código

A formatação usa indentação de quatro espaços nos fontes Java e Kotlin, com ferramentas oficiais fixadas por versão e SHA-256. É uma etapa opcional de desenvolvimento; a compilação não depende destes formatadores.

Requisitos: Python 3.9 ou superior e **JDK 21** para os formatadores. O requisito do formatador é separado do JDK aceito pelo build Android.

```sh
# Primeira execução: baixa apenas os JARs oficiais e formata os fontes locais.
./scripts/format-code.sh --download

# Conferência, sem alterar arquivos e sem baixar ferramentas automaticamente.
./scripts/format-code.sh --check

# Cache alternativo fora do checkout.
./scripts/format-code.sh --download --tools-dir /caminho/para/formatadores
./scripts/format-code.sh --check --tools-dir /caminho/para/formatadores
```

Os JARs ficam por padrão em `build/formatters/`, ignorado pelo Git. Não são publicados no código nem nos APKs. `JAVA_HOME`, quando definido, seleciona o JDK. Nenhum fonte é enviado a um serviço remoto: a rede é usada somente pelo `--download` para obter as ferramentas, e o script valida o checksum antes de executá-las.

| Linguagem | Ferramenta | Estilo |
| --- | --- | --- |
| Java | [google-java-format 1.37.0](https://github.com/google/google-java-format/releases/tag/v1.37.0) | AOSP, quatro espaços |
| Kotlin | [ktfmt 0.65](https://github.com/Kotlin/ktfmt/releases/tag/v0.65) | Kotlin language guidelines, quatro espaços |

O script preserva imports não utilizados. No Java, também preserva a ordem dos imports, a ordem dos modificadores e os literais longos. Kotlin pode ajustar separadores e posição dos imports conforme seu formatador. Não são introduzidos recursos, pacotes ou mudanças intencionais de lógica nessa etapa.

Antes de contribuir, execute a conferência e os checks de build indicados em [BUILDING.md](BUILDING.md). Formatação aprovada não substitui compilação, lint ou os testes funcionais exigidos pela alteração.

## Fontes webOS

Os fontes próprios em `webos/app/` e `webos/service/` precisam continuar compatíveis com ES5. A configuração `.prettierrc.json` desativa vírgulas finais nesses diretórios, inclusive em chamadas de função. Após formatar, execute `npm ci --prefix webos` e `npm test --prefix webos`; `node --check` em um Node recente não basta para comprovar compatibilidade com o runtime antigo da LG. O SDK de fornecedor mantém seu arquivo e créditos originais.
