# LG webOS: conta, Developer Mode e diagnóstico

Atualizado em **27/10/2026**. Este guia prepara uma LG para desenvolvimento e registra os testes realizados. O APK OwnTV é Android e não funciona no webOS; o [piloto webOS](../webos/README.md) usa um pacote IPK separado.

Veja também a [matriz Android × LG e a lista de pendências](STATUS.pt-BR.md), incluindo Home físico e seleção de favoritos.

## Estado do suporte

| Etapa | Situação |
| --- | --- |
| Conta LG e Developer Mode | Configurados na instalação de referência |
| SSH de desenvolvedor | Autenticação e consultas de leitura verificadas em seis LG 43LM631C0SB |
| Hardware e firmware | Consultados diretamente nos aparelhos; resumo abaixo |
| Root e SSH persistente após expiração do modo dev | Não aplicados nem validados |
| Substituição da Home LG | Não implementada nem validada |
| OwnTV para webOS | Versão experimental 0.2.1 com seleção de favoritos, notícias, sugestões e câmeras; veja o [registro de validação](../releases/webos/0.2.1/README.md). O APK Android não é instalável |

## 1. Conta de desenvolvedor

No computador, abra [LG webOS Developer](https://webostv.developer.lge.com/) e use **SIGN IN**. Entre com sua conta LG ou crie uma, confirme o e-mail se solicitado e conclua o cadastro de desenvolvedor apresentado pelo portal. Leia e aceite pessoalmente os termos exigidos antes de usar o serviço. Depois use a mesma conta no aplicativo da TV.

### Erro de conta brasileira em serviços dos EUA

Se o navegador informar que a conta foi registrada no Brasil e pedir outra conta para serviços dos EUA:

1. Volte ao formulário de login e clique em **U.S.A / English** no topo.
2. Abra **CENTRAL & SOUTH AMERICA**.
3. Escolha **Brazil — Português**.
4. Entre novamente com a conta brasileira.

Esse caminho foi conferido na página de login. Corrige a região do formulário; não exige recriar a conta nem mudar o país da TV. Se persistir, reabra o login pelo portal Developer e confira o seletor antes de entrar.

## 2. Preparar a TV

Com TV e computador na mesma rede, instale o aplicativo oficial **Developer Mode**, da LG, pela loja da TV. Faça login, habilite **Dev Mode Status** e aguarde a reinicialização. Reabra o aplicativo e habilite **Key Server**. Anote privadamente o endereço IP e a **Passphrase**, respeitando maiúsculas/minúsculas.

A sessão tem prazo: consulte **Remain Session** e use **EXTEND** antes de expirar. O modo pode ser desativado após reinício com prazo vencido ou após dez reinícios sem rede; os aplicativos instalados pelo modo desenvolvedor são removidos quando ele é desativado. Consulte as [instruções oficiais da LG](https://webostv.developer.lge.com/develop/getting-started/developer-mode-app).

## 3. Preparar o computador e conectar

Instale Node.js/npm em uma versão compatível com a [webOS CLI oficial](https://webostv.developer.lge.com/develop/tools/cli-installation), conferindo os requisitos atuais da LG. No terminal:

```sh
npm install -g @webos-tools/cli
ares -V
ares-setup-device
```

No assistente, escolha **add**, dê um nome como `lg-piloto`, informe o IP exibido na TV, porta **9922** e usuário **prisoner**. Não use a senha da conta LG como senha SSH. Confira o cadastro e obtenha a chave:

```sh
ares-setup-device --list
ares-novacom --device lg-piloto --getkey
# Informe a Passphrase somente quando solicitada.
ares-device --system-info --device lg-piloto
```

Para várias TVs, use um nome por aparelho e sempre informe `--device`. Cada TV possui sua própria chave e Passphrase. Um login bem-sucedido em outro Mac não transfere automaticamente as chaves SSH para este computador.

A sequência CLI acima segue a documentação oficial. Nesta instalação, os testes foram realizados por cliente SSH direto, com as chaves fornecidas pelo Key Server, e não por execução desta sequência da CLI.

## 4. Diagnóstico do acesso

| Sintoma | Conferir |
| --- | --- |
| Conexão recusada ou timeout | TV ligada, IP atual, mesma rede, ausência de isolamento entre clientes e Dev Mode ativo |
| Falha ao obter chave | Key Server ativo; a porta 9991 serve a chave, enquanto 9922 recebe SSH |
| Chave não abre | Passphrase atual e pertencente à TV correta, respeitando maiúsculas/minúsculas |
| SSH antigo incompatível | Prefira a CLI atual; no OpenSSH, habilite compatibilidade RSA somente para a conexão dessa TV |
| Aplicativo desapareceu | Confira expiração/desativação do Developer Mode |
| APK não instala | APK é Android; webOS usa outro formato e outra implementação |

Se já possui a chave de desenvolvedor em um arquivo privado, o OpenSSH pode conectar assim, substituindo `TV_IP` pelo endereço local da TV:

```sh
chmod 600 ./webos_rsa
ssh -i ./webos_rsa -p 9922 prisoner@TV_IP
```

Nas unidades testadas, o servidor exigiu compatibilidade com `ssh-rsa`. Se aparecer o erro correspondente, use somente nessa conexão:

```sh
ssh -i ./webos_rsa -p 9922 \
  -o HostKeyAlgorithms=+ssh-rsa \
  -o PubkeyAcceptedAlgorithms=+ssh-rsa \
  prisoner@TV_IP
```

Não desative a verificação de identidade do servidor. O Key Server pode ser desligado após importar a chave; isso não desativa o Developer Mode. A chave deverá ser obtida novamente se for substituída pela TV.

## 5. Hardware verificado

Leituras realizadas em seis unidades do mesmo modelo, em 27/10/2026. Não incluem dados identificadores da rede ou dos proprietários.

| Item | Resultado |
| --- | --- |
| Modelo / placa | 43LM631C0SB / M3R_ATSC_KR |
| Firmware / SDK | 05.40.97 / 4.10.0 |
| Plataforma / CPU | m5621 / quatro núcleos Cortex-A53 ativos |
| Sistema em execução | ARM de 32 bits, Linux 4.4.84 |
| Clock | Não confirmado pelo acesso disponível |
| RAM física | 1 GiB nos parâmetros de inicialização |
| RAM utilizável pelo Linux | Aproximadamente 658 MiB |
| RAM disponível na medição | 162–183 MiB; varia com os aplicativos |
| Swap | Aproximadamente 500 MiB; não representa RAM física adicional |
| Armazenamento interno | Cerca de 4 GB nominais, 3,64 GiB endereçáveis |
| Partição acessível para aplicativos | Aproximadamente 605 MiB, com 433–505 MiB livres na medição |
| GPU | Driver Mali kbase; modelo específico não confirmado |
| Rede sem fio | Driver MT7668 e driver Bluetooth carregados |

A CPU foi identificada por `CPU part 0xd03`, conforme a [referência ARM](https://community.arm.com/oss-platforms/w/docs/528/cpufreq-dvfs). A [ficha oficial LG](https://www.lg.com/br/tvs-e-soundbars/smart-tvs/43lm631c0sb/) especifica tela LCD/LED de 43 polegadas Full HD a 60 Hz, HDR10, três HDMI, duas USB, Bluetooth e áudio estéreo de 10 W. O nome comercial webOS 4.5 e o SDK 4.10.0 são identificações diferentes; não interprete o firmware 05.40.97 como webOS 5.

A futura interface precisa respeitar esse orçamento de memória: imagens para Full HD, poucos efeitos simultâneos e processamento pesado de câmeras em servidor externo. Isso é orientação de projeto, não resultado de um teste de desempenho do OwnTV em webOS.

## 6. Root e substituição da Home

O Developer Mode fornece acesso como `prisoner`, não como root. Não garante SSH permanente nem substitui a tela inicial. Em 27/10/2026, a consulta do modelo exato com firmware 05.40.97 no [CanI.RootMy.TV](https://cani.rootmy.tv/) não encontrou o modelo; isso não comprova compatibilidade ou incompatibilidade com root.

Há métodos comunitários como [faultmanager-autoroot](https://github.com/throwaway96/faultmanager-autoroot), cuja compatibilidade depende de firmware e plataforma. Não foram executados nestas TVs. Não há procedimento de root validado pelo OwnTV para esse modelo nesta entrega. Qualquer validação futura deve começar em uma unidade, registrar resultado e reversão e atualizar este guia antes de expandir para outras TVs.

## 7. Dados privados e manutenção

Não publique fotos da tela Developer Mode com Passphrase, chaves SSH, IPs/MACs reais, contas LG, logs privados, configurações ou imagens de câmeras. Use nomes e endereços substituíveis nos exemplos. Guarde material de acesso fora do repositório e restrinja suas permissões locais.

Mantenha o [README](../README.md#compatibilidade), este guia e o [changelog](../CHANGELOG.md) alinhados a cada mudança. Separe sempre o que foi testado, o que é instrução oficial e o que permanece uma proposta. Veja também a [instalação Android](INSTALLATION.pt-BR.md).

## Piloto visual e navegador

A implementação inicial segue a composição Android, com limites descritos no [guia do piloto](../webos/README.md). O navegador LG abriu o site solicitado, mas a renderização foi relatada como quebrada. Navegador, URL inicial, execução automática de páginas e tela cheia web foram retirados do escopo por solicitação do usuário. A [LG documenta Chromium 53 para webOS 4.x](https://webostv.developer.lge.com/develop/specifications/web-api-and-web-engine); não se presume compatibilidade com sites modernos.

## Relógio, acesso e versão do sistema — conferência de 27/10/2026

Nas seis unidades, `useNetworkTime=true`, `useNetworkTimeZone=true`, fuso `America/Sao_Paulo` (UTC−03) e `timeValid=true`. Os relógios ficaram a poucos segundos do Mac durante a leitura. O sistema informou fonte `sdp`; isso confirma a configuração automática e o horário observado, mas **não identifica um servidor NTP nem comprova uma sessão NTP**. A API de configurações gerais negou acesso ao usuário de desenvolvimento. A consulta `time/getNTPTime` retornou `NTP requests are prohibited at the moment` nas seis TVs. Não foi instalado ou alterado um daemon de horário.

Todas permitem SSH com shell BusyBox como `prisoner`, pela porta 9922 e chave individual do Developer Mode. O usuário não é root, não pode escrever no cadastro de contas e não disponibiliza `adduser`/`useradd` nesse ambiente. **Não foi criado um usuário adicional**: contas de sistema e persistência dependem de privilégios que não estão disponíveis. Um serviço com nome alternativo dentro do aplicativo não equivaleria a uma conta independente do sistema.

O firmware lido nas seis é **05.40.97**. A [página oficial LG de firmware desta família](https://www.lg.com/ar/soporte/producto/lg-43LM6300PSB.KWNYLWK), cuja lista inclui explicitamente 43LM631C0SB, oferece 05.40.97 com data de 07/07/2025. É a versão pública mais recente encontrada nessa página; não foi executada atualização de firmware nem confirmado um canal OTA diferente para cada unidade.

Android no processador ARM seria tecnicamente concebível mediante port específico, mas não foi encontrada uma imagem Android para esta placa LG. Bootloader, kernel, drivers e HALs precisam corresponder ao hardware; root em webOS não fornece isso. Veja a [arquitetura AOSP](https://source.android.com/docs/core/architecture). Um equipamento Android externo por HDMI permite usar a versão Android do OwnTV sem trocar o sistema interno da TV.

## Notícias, sugestões e câmeras — OwnTV 0.2.0

O [guia webOS](../webos/README.md) descreve a instalação do app e serviço no mesmo IPK. Notícias vêm do RSS Google Notícias para os temas escolhidos; sugestões vêm dos canais públicos do YouTube configurados. Não acessam histórico, senhas ou recomendações pessoais da conta.

Para câmeras, instale a [ponte privada no computador/servidor](../webos/bridge/README.md) e configure somente endereço e chave da ponte em cada TV. O gravador mantém seu usuário/senha no servidor; esses valores nunca devem entrar no IPK, exemplos, capturas ou Git. O mosaico mostra seis JPEGs atualizados por página e o player abre um HLS por TV. A [LG documenta HLS](https://webostv.developer.lge.com/develop/specifications/streaming-protocol-drm); o port não tenta executar a biblioteca RTSP Android no webOS.

No macOS, o serviço automático pode precisar de autorização em **Ajustes do Sistema → Privacidade e Segurança → Rede Local**. O funcionamento iniciado pelo terminal não comprova que o LaunchAgent tem a mesma autorização. Confira a imagem real na TV, reinício do serviço e operação após login antes de depender da inicialização automática.

## Aplicativos favoritos — OwnTV 0.2.1

A lista dos apps da própria LG agora pode ser consultada pelo pareamento de controle remoto. Em **Editar favoritos**, escolha **Atualizar apps**, permita o aviso na TV e marque seus aplicativos com OK. **Ordenar favoritos** permite mover os escolhidos com as setas. Veja [o guia de pareamento, navegação e segurança](LG-FAVORITOS.pt-BR.md). O botão Home físico continua sendo da LG: `setDefaultApp` respondeu `Unknown method` nas seis unidades.
