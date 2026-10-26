# Revisão de segurança — OwnTV 0.2.3

Revisão em 25/10/2026. Escopo: código publicável, histórico público anterior, recursos, ferramentas de build, dependências, APK e superfícies de rede do aplicativo. Não é um pentest externo, uma certificação ou garantia de ausência de falhas desconhecidas. Os metadados da entrega ficam em `releases/0.2.3/`.

## Dados excluídos da publicação

Não foram encontradas configurações reais de câmeras, endereços privados de equipamentos, usuários/senhas de DVR, URLs RTSP autenticadas, backups de TV, chaves de assinatura, tokens, credenciais SSH ou capturas de câmeras particulares na árvore publicável. Os dois commits anteriormente publicados também foram examinados. Os endereços presentes nos testes de rede são valores sintéticos para testar faixas IPv4/IPv6, não configurações de uma instalação.

O código contém a implementação da configuração de câmeras, não os dados preenchidos pelo usuário. Esses dados continuam no armazenamento privado da TV, protegidos por AES-GCM e Android Keystore. Nenhuma exportação dos dados da TV é necessária para compilar ou publicar o OwnTV.

Os arquivos editáveis de marca foram retirados da distribuição e o histórico público foi reconstruído. O APK usa PNGs renderizados, sem os desenhos dos logos em SVG, VectorDrawable ou strings de caminhos no código. Imagens de documentação foram inspecionadas. A remoção de referências Git não apaga cópias já baixadas nem garante eliminação imediata de objetos em cache no provedor; consulte [a documentação do GitHub](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository).

## Correções aplicadas

| Área | Correção |
| --- | --- |
| HTTP/TLS | HTTP claro desabilitado e política Android vinculada ao manifesto; validação de certificados/hostname mantida. O nome herdado `trustAllHttpClient` foi corrigido: a implementação já validava TLS. |
| SSH | Rejeição de peers públicos, oito conexões totais, duas sessões autenticadas, quatro canais por sessão, três tentativas por conexão, seis por endereço/minuto e 24 globais/minuto. Hash só após limites; senha até 1024 caracteres; timeout de autenticação de 30 segundos e inatividade de dez minutos. |
| Importações locais | Backup limitado a 1 MiB; foto a 35 MiB, com dimensões verificadas e substituição atômica. Uma falha preserva o arquivo anterior. |
| Wallpapers distribuídos | Remoção de EXIF/GPS/XMP/IPTC/comentários, preservando pixels, resolução, orientação e perfil ICC. Nenhuma foto da residência é incluída. |
| Download de fotos no computador | HTTPS e host Wikimedia permitido, inclusive em redirecionamentos; tamanho e identificadores limitados; gravação atômica; sanitização antes do build. |
| Gradle | Wrapper e scripts oficiais 8.9; JAR conferido contra hash oficial; checksum da distribuição fixado e validação de URL habilitada. |
| Bibliotecas de build | Atualização de Kotlin e versões transitivas sinalizadas na consulta inicial; R8 atualizado conforme a matriz de suporte oficial. |

## Verificações reproduzíveis

```sh
python3 scripts/check-public-assets.py
./scripts/test-ssh-security.sh
python3 scripts/test-image-privacy.py
./scripts/format-code.sh --check
./scripts/build-release.sh --local-sign
```

A conferência de formatação requer os formatadores opcionais previamente obtidos conforme [FORMATTING.md](FORMATTING.md). A assinatura local serve à compatibilidade com a instalação inicial; outra distribuição deve usar sua própria chave privada, conforme [BUILDING.md](BUILDING.md).

- **Gitleaks 8.30.1:** histórico e exportação dos arquivos versionados; extração textual do APK. A ferramenta é executada localmente, sem enviar código a um scanner remoto. Revisão manual complementou a detecção automática.
- **OSV:** consulta por coordenadas Maven efetivamente resolvidas. Apenas nomes e versões são enviados à [API pública](https://google.github.io/osv.dev/post-v1-querybatch/); código, imagens e credenciais não são enviados. A contagem e o resultado finais estão no relatório da release. Ausência de resultado depende da cobertura da base naquela data.
- **SSH:** testes JVM das faixas IPv4/IPv6, comprimento de senha, limites, expiração da janela e concorrência de cem chamadas.
- **Imagens:** testes de JPEG sequencial/progressivo, PNG, orientações, preservação dos dados comprimidos e ICC, arquivos malformados, limites, links simbólicos e URLs rejeitadas. Os quinze wallpapers do pacote foram comparados antes/depois.
- **APK:** build e lint, manifesto/pacote/versão, ausência de depuração, assinatura, conteúdo do ZIP, recursos de marca e ausência de dados privados. O checksum identifica o binário efetivamente entregue.

## Limites de operação

SSH autenticado executa como o usuário Android do OwnTV e consegue acessar arquivos do aplicativo; a raiz virtual SFTP não isola o terminal. Aceite apenas administradores confiáveis. O filtro de redes privadas não impede que um NAT masque a origem de uma conexão: não exponha o serviço na internet.

RTSP tradicional continua sem TLS. Criptografar as credenciais armazenadas não criptografa o vídeo na rede. O OwnTV não expõe um servidor de câmera nem publica a configuração do DVR; use uma rede confiável e permissões adequadas no próprio DVR.

A consulta de dependências não cobre o firmware da TV, bibliotecas nativas do sistema, todos os comportamentos de fornecedores ou vulnerabilidades ainda não divulgadas. O modelo de CI incluído em `docs/ci/` está inativo; não foi alegada aprovação de CI remota. IoT/Alexa permanece uma proposta documentada e não adiciona serviços de rede nesta versão.
