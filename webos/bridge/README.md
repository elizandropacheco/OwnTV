# Ponte privada de câmeras

O webOS recebe JPEG para as seis prévias e HLS para o vídeo ampliado. O servidor consulta o gravador com autenticação Digest e converte seu RTSP para HLS usando FFmpeg. A senha do gravador permanece **no servidor**; cada TV recebe uma chave própria da ponte. O servidor precisa ficar ligado, conectado à mesma rede autorizada e sem suspensão durante o uso.

Compatibilidade testada: gravador com caminhos Dahua/Intelbras `cgi-bin/snapshot.cgi` e `cam/realmonitor`, vídeo H.264, fluxo principal 1080p e substream. Isso não representa suporte automático a todos os gravadores, RTSPs ou serviços em nuvem.

## Preparar

Requisitos: Python 3.10 ou superior e FFmpeg. No macOS com Homebrew:

```sh
brew install python ffmpeg
mkdir -p "$HOME/Library/Application Support/OwnTV/bridge"
chmod 700 "$HOME/Library/Application Support/OwnTV/bridge"
umask 077
cp webos/bridge/config.example.json "$HOME/Library/Application Support/OwnTV/bridge/private.json"
python3 -c 'import secrets; print(secrets.token_urlsafe(32))'
```

Edite **o arquivo privado**, fora do checkout. Troque todos os exemplos:

- `bind`: IP local do servidor na interface alcançável pelas TVs. Não use `0.0.0.0`.
- `allowed_clients`: IPs das TVs autorizadas. Acrescente o IP do computador somente se precisar testar nele.
- `tokens`: uma chave aleatória diferente por TV, gerada pelo comando acima. Não reutilize a senha do gravador.
- `recorder`: endereço, usuário com permissão de leitura, senha, portas e quantidade de canais.
- `transcode`: URLs legadas podem copiar H.264 quando `false`. O player OwnTV solicita enquadramento explícito, que requer conversão. Por padrão usa libx264; para aceleração no Mac, acrescente `"encoder": "h264_videotoolbox"` no nível principal do JSON e teste a disponibilidade do hardware.
- `http_base`: opcional, para um endpoint HTTP/HTTPS diferente no gravador. Sem esse campo, usa HTTP na porta `http_port`.

Reserve IPs no DHCP ou atualize a configuração quando mudarem. Não publique este arquivo, tokens, fotos de câmeras ou chaves Developer Mode. Proteja também backups desse diretório.

## Testar e conectar a TV

```sh
chmod 600 "$HOME/Library/Application Support/OwnTV/bridge/private.json"
python3 webos/bridge/camera_bridge.py --config "$HOME/Library/Application Support/OwnTV/bridge/private.json"
```

Em cada OwnTV: **Configurações → Câmeras → Endereço da ponte**, informe `http://IP_DO_SERVIDOR:8788` e a chave daquela TV. Salve e abra Câmeras. O endereço é a base, sem `/t/` ou caminhos de vídeo. Não informe a senha do gravador na TV. A chave é armazenada pelo serviço privado do OwnTV; outras aplicações não podem consultar esse método pelo barramento.

## Iniciar automaticamente no Mac

Encerre primeiro a execução manual com Ctrl+C, para liberar a porta. Execute no usuário que permanecerá conectado:

```sh
python3 webos/bridge/install_macos.py --config "$HOME/Library/Application Support/OwnTV/bridge/private.json"
launchctl print "gui/$(id -u)/io.elizandropacheco.owntv.camera-bridge"
```

O instalador copia o código e a configuração para Application Support e registra um LaunchAgent. Inicia no login desse usuário e reinicia se encerrar. Não inicia antes do login, não impede a suspensão do Mac e não transforma o computador em servidor sempre ligado. Após atualizar o código da ponte, execute o instalador novamente. No Linux, execute o mesmo `camera_bridge.py` com um gerenciador de serviços e um usuário dedicado, mantendo o arquivo privado fora do repositório.

Para parar e desabilitar no macOS:

```sh
launchctl bootout "gui/$(id -u)/io.elizandropacheco.owntv.camera-bridge"
rm "$HOME/Library/LaunchAgents/io.elizandropacheco.owntv.camera-bridge.plist"
```

A configuração privada permanece disponível para reinstalação; remova-a separadamente se não precisar mais.

## Operação e segurança

- Mosaico com seis JPEGs, atualizados a cada três segundos. Não são seis vídeos contínuos.
- Um vídeo ao vivo por TV, principal ou leve, com áudio inicialmente silenciado e preenchimento padrão.
- Streams iguais são compartilhados entre TVs. Até doze combinações canal/qualidade/enquadramento simultâneas, permitindo a transição das seis TVs; conexões ociosas são encerradas após cerca de 35 segundos.
- HLS tem atraso de alguns segundos e a primeira conexão pode levar de 20 a 45 segundos, especialmente ao abrir vários canais juntos. Não é reprodução sem latência.
- No player OwnTV, o fluxo principal é normalizado para 1920×1080 e o leve para 960×540. Preencher corta as sobras; ajustar mantém a proporção com faixas. Uma fonte menor não ganha detalhes pela ampliação. URLs legadas sem enquadramento podem remultiplexar H.264. Áudio, quando disponível, é convertido para AAC. Sem áudio na origem, ativar áudio não cria uma faixa.
- Buffer temporário de poucos segmentos, removido ao encerrar; a ponte não mantém arquivo de gravações.
- Acesso exige IP permitido **e** token. HTTP/RTSP na LAN não cifram o transporte; use rede confiável/VLAN. Não abra a porta na internet. Para outra rede, use VPN ou um proxy HTTPS administrado por você.
- A chave da ponte é uma credencial de acesso a todas as câmeras configuradas. Não compartilhe URLs de mídia; elas incluem a chave. Os logs HTTP estão desativados para não registrá-la.
- Troque a chave comprometida no servidor e na TV. `Desconectar` remove a chave daquela instalação OwnTV, mas a revogação efetiva ocorre removendo-a da lista `tokens` do servidor e reiniciando a ponte.

Os testes automatizados de rotas e autenticação estão em `webos/tests/bridge_test.py`. Capturas de câmeras reais não fazem parte dos testes públicos.

### Permissão de rede no macOS

Se o modo manual funcionar e o LaunchAgent retornar câmera indisponível, confira **Privacidade e Segurança → Rede Local** para Python/FFmpeg. O macOS pode tratar o serviço em segundo plano de forma diferente do terminal. Se a entrada não estiver disponível, mantenha a execução manual já autorizada ou mova a ponte para um servidor. Não desative a proteção global nem considere o autostart validado só porque o processo iniciou.

A ponte limita a espera de E/S RTSP a 15 segundos e reinicia processos com playlist parada. Sequências HLS usam a hora de início para não reutilizar números após reiniciar. O player detecta falta de avanço e tenta reconectar até duas vezes; depois apresenta a falha e permite Reconectar ou Modo leve.
