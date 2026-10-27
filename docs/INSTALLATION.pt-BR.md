# Instalar e configurar o OwnTV

Este guia usa Android TV/Google TV. Os nomes dos menus variam entre fabricantes e versões. A instalação foi validada em uma TCL com Android TV 14. Não exige root, troca de firmware ou remoção do launcher original.

Para LG/webOS, use o [guia específico de conta de desenvolvedor e acesso à TV](LG-WEBOS.pt-BR.md). Os comandos ADB e o APK deste guia são exclusivos do Android.

## 1. Obter o APK

Baixe o APK e o arquivo SHA-256 em [Releases](https://github.com/elizandropacheco/OwnTV/releases). Se não houver uma versão para seu aparelho, [compile o código](../README.md#compilar). O identificador do OwnTV é `io.elizandropacheco.tv`.

Compare a soma do arquivo baixado com o SHA-256 da versão:

```sh
# macOS
shasum -a 256 OwnTV-0.2.4.apk
# Linux
sha256sum OwnTV-0.2.4.apk
```

```powershell
# Windows PowerShell
Get-FileHash .\OwnTV-0.2.4.apk -Algorithm SHA256
```

Para atualizar uma instalação existente, preserve a chave de assinatura. A versão disponibilizada neste projeto usa uma assinatura local; uma compilação própria com outra chave não atualiza essa instalação. Não desinstale para contornar esse erro sem antes planejar a perda dos dados locais e das credenciais do DVR.

## 2. Instalar sem computador

1. Copie o APK para um pendrive ou transfira-o para a TV por um método de sua confiança.
2. Abra o arquivo com um gerenciador de arquivos na TV.
3. Se solicitado, abra **Configurações → Aplicativos → Acesso especial → Instalar apps desconhecidos** e autorize apenas o gerenciador usado. Alguns firmwares chamam esse menu de **Segurança e restrições → Fontes desconhecidas**.
4. Confirme a instalação e abra o OwnTV. Depois você pode retirar a autorização de fontes desconhecidas.

A instalação por arquivo não exige habilitar o modo desenvolvedor. Para instalar e diagnosticar pelo computador, siga as próximas etapas.

## 3. Ativar o modo desenvolvedor

1. Na TV, abra **Configurações → Sistema → Sobre**. Em versões anteriores, use **Preferências do dispositivo → Sobre**.
2. Localize **Compilação do SO Android TV**, **Compilação** ou **Build**. Não é o nome comercial/modelo da TV.
3. Pressione **OK sete vezes**, até aparecer a confirmação de que você é desenvolvedor. Se houver um PIN do sistema, informe-o na própria TV.
4. Volte a **Sistema/Preferências do dispositivo → Opções do desenvolvedor**.
5. Ative **Depuração USB/ADB**. Em alguns aparelhos TCL, essa opção também permite ADB pela rede; em outros há uma opção separada de **Depuração de rede** ou **Depuração sem fio**.

Não altere limite de processos, renderização, resolução, animações ou outras opções para instalar o OwnTV. Não habilite desbloqueio de OEM.

Referência: [opções do desenvolvedor — Android Developers](https://developer.android.com/studio/debug/dev-options).

## 4. Preparar o computador

Baixe o [SDK Platform Tools oficial](https://developer.android.com/tools/releases/platform-tools), disponível para Windows, macOS e Linux, e extraia o pacote. Abra um terminal dentro da pasta `platform-tools`.

Nos exemplos, `adb` pressupõe que a ferramenta está no PATH. Caso contrário, use `./adb` no macOS/Linux e `.\adb.exe` no PowerShell. TV e computador devem estar na mesma rede local, sem isolamento de clientes/visitantes.

Descubra o IP da TV em **Configurações → Rede e internet → Rede conectada → Endereço IP**. Use esse endereço nos exemplos, substituindo `TV_IP`.

## 5. Conectar por ADB

### A. TV com depuração de rede na porta 5555

Use esta opção somente se o firmware oferecer ADB TCP nessa porta. Foi o método validado na TCL desta instalação.

```sh
adb connect TV_IP:5555
adb devices -l
```

Na TV, confirme a impressão digital RSA do computador. Se for seu computador pessoal, pode selecionar **Sempre permitir deste computador**. A lista deve mostrar `device`; `unauthorized` significa que falta a confirmação na TV.

### B. TV com pareamento por código

Se existir **Depuração sem fio → Parear dispositivo com código**, ative a opção, abra a tela de pareamento e use o IP/porta exibidos nela:

```sh
adb pair TV_IP:PORTA_DE_PAREAMENTO
# Digite o código que aparece na TV.
adb connect TV_IP:PORTA_DE_DEPURACAO
adb devices -l
```

A porta de pareamento e a porta de depuração são diferentes. Use a segunda porta mostrada na tela principal da depuração sem fio para `adb connect`; não suponha que seja 5555.

### C. USB

A conexão USB depende de uma porta com função de depuração suportada pelo fabricante. A porta USB comum de mídia de uma TV pode não aceitar ADB; conectar um cabo não garante essa função. Quando houver USB de depuração autorizado, `adb devices -l` deve listar a TV. A documentação do Android também descreve `adb tcpip 5555` após uma conexão USB válida; esse comando não cria acesso remoto a um aparelho ainda não autorizado.

Referências: [ADB](https://developer.android.com/tools/adb) e [conectar dispositivos físicos](https://developer.android.com/studio/run/device).

## 6. Instalar e abrir

Com apenas uma TV conectada:

```sh
adb install -r OwnTV-0.2.4.apk
adb shell am start -n io.elizandropacheco.tv/com.conreo.couchytv.MainActivity
adb shell pm grant io.elizandropacheco.tv android.permission.READ_TV_LISTINGS
```

Com mais de um dispositivo, acrescente `-s TV_IP:PORTA` depois de `adb` em cada comando. A permissão de listas permite ler programas compartilhados pelos aplicativos; ela não concede acesso à conta ou ao histórico privado do YouTube.

O parâmetro `-r` atualiza o aplicativo preservando seus dados quando a assinatura corresponde. O APK de release é otimizado e não permite `run-as`; isso é esperado.

## 7. Fazer o botão Home abrir OwnTV

1. Se o sistema oferecer uma escolha de aplicativo de início, selecione **OwnTV → Sempre**.
2. Em firmwares que retornam ao Google TV/TCL, abra **Configurações do Android → Acessibilidade → OwnTV Home** e ative o serviço.
3. Se houver controle de inicialização automática/energia da TCL, permita a inicialização do OwnTV.
4. Pressione **Home** e confira o resultado. Se o firmware revogar a acessibilidade após reiniciar, reative-a nesse mesmo menu.

O serviço tenta interceptar somente a tecla Home antes da ação padrão e também observa a atividade Home do launcher original como alternativa. Não captura nem registra o conteúdo das telas. Alguns firmwares TCL mantêm o launcher original com prioridade mesmo após escolher outro aplicativo Home. Se houver um breve flash da Home original, a alternativa de redireção pode estar sendo usada. Depois de atualizar a versão e sua capacidade de filtrar teclas, pode ser necessário desativar/reativar o serviço na acessibilidade. O launcher original permanece instalado; não é necessário desabilitar pacotes do sistema.

## 8. Personalização inicial

- **Favoritos:** use **Personalizar apps → Seções** para incluir/remover aplicativos. Use **Configurações → Personalizar sua central → Ordem dos favoritos** para mover antes/depois ou ao início/fim. A entrada no dock seleciona o primeiro item à esquerda. ↓ no menu superior vai diretamente aos favoritos; ↑ nos favoritos seleciona Câmeras; outro ↑ seleciona PS5 e outro ↑ retorna ao menu superior.
- **Cores dos favoritos:** **Configurações → Personalizar sua central → Ícones dos favoritos** alterna colorido por marca (padrão) e monocromático.
- **Aparência:** escala, ícones, cantos, espaçamento, labels e escurecimento ficam em **Personalizar apps → Tela**. O painel pode ser 4K mesmo quando o Android usa uma interface 1080p; OwnTV respeita a resolução configurada pelo fabricante.
- **Logo:** **Configurações → Personalizar sua central → Identidade visual e logo** alterna EP, monograma OwnTV e imagem PNG/JPEG/WebP local ou USB na Home, nos menus e na abertura. A identidade EP é o padrão; **Pré-visualizar abertura** permite conferir a animação nessa mesma página.
- **Cards:** **Cards e jogos → Cards principais da Home/Menu de jogos** edita título, imagem, ação, alinhamento e intensidade. O seletor de arquivo pode usar armazenamento local ou USB.
- **Notícias:** **Configurações → Notícias e temas** permite até oito temas separados por vírgulas e intervalo de alternância. Os padrões são telecom, tecnologia e inteligência artificial. As manchetes vêm do RSS do Google Notícias.
- **Conteúdos:** as linhas exibem quatro sugestões completas por página. O YouTube tem prioridade; vídeos diretos dependem do conteúdo que ele publicar para launchers. Veja [conteúdos e YouTube](../README.md#conteúdos-e-youtube).
- **PS5:** escolha a entrada em **Jogos → HDMI do PS5**. Ative 120 Hz/120 FPS no console/jogo quando TV, porta e cabo forem compatíveis. O launcher abre ajustes; não acrescenta suporte de hardware.
- **Câmeras:** abra **Câmeras → Opções do DVR → Configurar**. Informe os dados somente na TV. O mosaico abre o fluxo principal com OK; **Preencher** é o enquadramento padrão. Nem todos os modelos proprietários usam o protocolo RTSP suportado.
- **Fundos:** **Configurações → Fundos personalizados** configura rotação e downloads. A coleção começa com 15 fotos originais quando incluída na compilação; downloads de 1/3/5 por dia têm limite de 25/50/100.
- **SSH/SFTP:** configure usuário, senha e porta em **Configurações → Acesso SSH e SFTP**. Esse acesso funciona como usuário do aplicativo e não substitui as permissões do ADB. Credenciais não fazem parte do APK nem do repositório.

## 9. Diagnóstico

| Sintoma | O que conferir |
| --- | --- |
| `adb` não encontrado | Abra o terminal em `platform-tools` ou configure o PATH; use o prefixo adequado ao sistema |
| `unauthorized` | A TV deve estar ligada e mostrar o aviso RSA; aceite o computador na própria TV |
| Conexão recusada/timeout | IP, porta de depuração, rede de visitantes, modo de espera e opção ADB ativa |
| `more than one device` | Use `adb -s TV_IP:PORTA ...` |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | A chave é diferente; use uma atualização assinada pela mesma chave e preserve os dados |
| `INSTALL_FAILED_VERSION_DOWNGRADE` | Use versão com código igual ou superior à instalada; não force downgrade como procedimento normal |
| Home continua na TCL | Confira aplicativo de início, acessibilidade OwnTV Home e inicialização automática |
| Sem recomendações de um aplicativo | Abra-o, confira login/consentimentos e permissão READ_TV_LISTINGS; um feed privado do Google TV pode não estar disponível para outro launcher |
| Câmera não abre | Confira rede/porta/credenciais/RTSP/modelo; teste o substream/modo leve e o fluxo principal separadamente |
| SSH não conecta | Confira status ativo, IP, porta, usuário/senha e mesma rede; o SSH do OwnTV não usa a porta ADB |

Para obter um log durante reprodução de erro, sem limpar o registro existente:

```sh
adb logcat -v threadtime > owntv-log.txt
# Reproduza o erro e pressione Ctrl+C.
```

Revise logs antes de publicá-los: outros aplicativos podem registrar dados pessoais. Não envie imagens de câmeras, credenciais, backups ou chaves para issues públicas.

## 10. Atualizar, desativar ou voltar ao original

- **Atualizar:** baixe a versão compatível com a assinatura atual e use `adb install -r` ou abra o APK na TV.
- **Desativar a depuração:** depois da instalação/testes, ela pode ser desligada nas opções do desenvolvedor. OwnTV não precisa de uma conexão ADB permanente para funcionar.
- **Desligar SSH:** use **Parar servidor SSH** na página do OwnTV; isso também desativa o reinício automático desse serviço.
- **Voltar ao Google TV:** use **Configurações → Abrir Google TV original** para acesso temporário. Para reverter permanentemente, desative **OwnTV Home** na acessibilidade e retire a preferência de aplicativo de início do OwnTV, se definida.
- **Desinstalar:** remove os dados locais do OwnTV, incluindo preferências e configuração das câmeras. Não remove o firmware ou launcher original.
