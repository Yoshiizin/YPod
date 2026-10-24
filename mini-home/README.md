# YPod Mini Home Dynamic — v4.1

Launcher Android ultraleve para o Samsung Galaxy Y Duos **GT-S6102B**, Android **2.3.6 (API 10)** e tela **240×320**.

## Funcionalidades

- Interface retrô azul inspirada no estilo mini-home, com ilustração vetorial de computador;
- Relógio digital em formato 24h, atualizado a cada minuto; toque nele para ver a versão ampliada animada;
- Dia, data e bateria atualizados pelo Android;
- Atalhos Music / Files / Settings / Apps; **toque longo** para vincular o Music ao player instalado;
- Lista de aplicativos com rolagem; gestos para abrir o menu e controles básicos;
- Ícones desenhados em Canvas (sem emojis incompatíveis com Android 2.3);
- **Correção v4.1:** zona de toque do botão Music separada da zona do relógio e tolerância a pequenos movimentos do dedo.

## Instalar

1. Abra [Build Mini Home Galaxy Y](https://github.com/Yoshiizin/YPod/actions/workflows/mini-home-apk.yml) no GitHub.
2. Entre em um build bem-sucedido, baixe **Artifacts** e extraia o APK `YPod-Mini-Home-Galaxy-Y.apk`.
3. Habilite Fontes desconhecidas no Galaxy Y, instale o arquivo .apk, aperte **Home** e selecione **YPod Mini Home 4.1**.
4. Se quiser abrir o app de música existente pelo atalho, **segure Music** e selecione o aplicativo da lista.

Pacote Android: `com.ypod.minihome.clockfix`. Esta edição instala **separadamente** das antigas Mini Homes e da Home Classic.

## Desenvolver

Código em `src/main/java/com/ypod/minihome/MiniActivity.java`. Build sem Gradle via `bash build.sh` com Android SDK Platform 28, Build Tools 35.0.0 e Java 17. `minSdkVersion=10`.

**Limites:** o APK é um launcher, não uma ROM. O build assina usando chave temporária; para atualizar sem desinstalar, é preciso configurar assinatura persistente. Também é preciso validar a interface em hardware físico.
