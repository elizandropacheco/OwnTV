# OwnTV 0.2.3

Código-fonte organizado e disponível diretamente no repositório, com scripts de build, formatação, testes de segurança e documentação de instalação e atualização. O build gera o APK e os metadados em `releases/0.2.3/`.

- Originais editáveis dos logos retirados da distribuição; identidade EP, monograma alternativo e abertura preservados com PNGs renderizados. A release anterior e sua tag foram retiradas, e a base pública foi reconstruída.
- Nenhuma configuração real de câmera, credencial, chave privada, backup de TV ou captura de câmera particular integra o código ou o APK.
- SSH reforçado com limites de conexão/autenticação e rejeição de endereços públicos; HTTP claro bloqueado; importações locais limitadas e atômicas.
- Wallpapers mantêm pixels e resolução, com metadados de captura/localização removidos. Downloads e ferramentas de build receberam verificações de integridade.
- Dependências do aplicativo e das ferramentas foram revisadas e atualizadas. Os resultados de build e segurança estão nos manifestos desta release.
- Estudo de gateway IoT e integração Alexa documentado. É um roteiro de implementação, sem novos serviços IoT ativos no APK.

[Instalação, modo desenvolvedor e ADB](https://github.com/elizandropacheco/OwnTV/blob/main/docs/INSTALLATION.pt-BR.md) · [Build e assinatura](https://github.com/elizandropacheco/OwnTV/blob/main/docs/BUILDING.md) · [Revisão de segurança](https://github.com/elizandropacheco/OwnTV/blob/main/docs/SECURITY_REVIEW.md) · [Estudo IoT/Alexa](https://github.com/elizandropacheco/OwnTV/blob/main/docs/IOT-ALEXA.md)

Use o APK e confira `SHA256SUMS.txt`. O APK é otimizado, não depurável e mantém a assinatura local da instalação inicial; não desinstale nem apague dados para contornar assinatura diferente. Chaves e dados de aparelhos não são distribuídos. O relatório de segurança registra o escopo e os limites da revisão, sem promessa de risco zero.

Desenvolvido por [Elizandro Pacheco](https://elizandropacheco.io/), com créditos ao Couchy Launcher de Conreo, sob GPLv3. [Siga no Instagram](https://www.instagram.com/elizandropacheco/), [inscreva-se no YouTube](https://www.youtube.com/@ElizandroPacheco) e dê uma estrela no OwnTV para acompanhar o projeto.
