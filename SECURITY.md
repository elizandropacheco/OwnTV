# Segurança e dados privados

OwnTV é um aplicativo Android com privilégios normais de aplicativo. A revisão de código, os scanners e os testes reduzem riscos; não constituem garantia de ausência de vulnerabilidades nem substituem um pentest independente.

## O que nunca deve entrar no repositório ou na release

- Endereços, usuários e senhas de câmeras, DVRs, SSH ou outros equipamentos reais.
- URLs RTSP com credenciais, arquivos de importação de câmeras, backups, dumps de dados da TV, tokens ou chaves privadas.
- Capturas e gravações de câmeras particulares.
- Originais editáveis de logos e documentos privados de criação.
- Logs de sessões que possam conter dados pessoais ou credenciais.

Configurações de câmera são preenchidas na própria TV. O aplicativo as armazena na área privada com AES-GCM e chave do Android Keystore. Não são recursos do APK nem exemplos do código. O backup Android do aplicativo está desabilitado. Exemplos de documentação usam identificadores genéricos.

## Serviços e rede

O SSH vem desativado em uma instalação nova e exige configuração local. Sua senha é verificada com PBKDF2-HMAC-SHA256 e comparação constante. Há limites de conexões, sessões, canais e tentativas de autenticação. Somente endereços privados, locais ou de loopback são aceitos pela política do serviço; isso não substitui um firewall, pois um redirecionamento/NAT pode esconder a origem real.

SSH dá acesso ao usuário Android do OwnTV. Embora o SFTP tenha uma raiz de transferência própria, o terminal não é uma jaula isolada: pode acessar arquivos privados que o usuário do aplicativo consegue acessar. Conceda esse acesso apenas a administradores confiáveis. Não há root, autorização ADB ou encaminhamento de portas. Desligue o serviço nas configurações quando não precisar dele.

HTTP em texto claro é bloqueado para clientes que respeitam a política Android. Conteúdos remotos devem usar HTTPS com validação de certificado e hostname. O aplicativo não contém um cliente TLS que aceite qualquer certificado.

RTSP tradicional usa seu próprio protocolo e pode transportar vídeo e autenticação sem TLS. A configuração de HTTP não o transforma em RTSPS. Use câmeras em rede confiável e não exponha portas do DVR, SSH ou ADB na internet. Credenciais locais criptografadas não tornam o transporte RTSP criptografado.

O serviço de acessibilidade é limitado à tecla Home e aos eventos de abertura do launcher original; não pode recuperar conteúdo das janelas. A página nativa de configurações e o serviço SSH não são componentes Android exportados. Os serviços exportados de acessibilidade e agendamento exigem permissões de vínculo do sistema.

## Antes de publicar

```sh
python3 scripts/check-public-assets.py
./scripts/test-ssh-security.sh
python3 scripts/test-image-privacy.py
./scripts/build-release.sh --local-sign
```

`--local-sign` é destinado à compatibilidade com a instalação local inicial. Para outra distribuição, configure sua chave conforme [o guia de build](docs/BUILDING.md); nunca publique a chave. Revise também o conteúdo efetivo do APK e o diff, não apenas `.gitignore`. A verificação de arquivos públicos é uma proteção específica contra arquivos privados conhecidos, não um scanner universal de segredos.

Confira [a revisão documentada](docs/SECURITY_REVIEW.md) para escopo, ferramentas, resultados e limites. Alterações futuras exigem nova revisão. Ao comunicar uma falha, não coloque dados reais de câmeras, tokens, dumps de aparelhos ou detalhes de acesso em uma issue pública; procure primeiro um canal privado do mantenedor pelo [site](https://elizandropacheco.io/).
