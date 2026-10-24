# Y-Dino — mini game para Galaxy Y Duos

Um runner arcade original do YPod, feito para o Samsung GT-S6102B.

- Android 2.3.6 / API 10, retrato 240 x 320;
- Sem internet, sem bibliotecas externas, sem anúncios;
- Toque na tela para pular fitas e boomboxes;
- Recorde salvo no armazenamento do aplicativo;
- Som simples opcional (SFX), acessível no canto superior direito;
- Botão II para pausa. Botão Voltar para pausa ou tela inicial;
- Interface desenhada em Canvas, sem imagens pesadas.

## Instalação
Baixe o artifact de GitHub Actions (APK) e transfira o arquivo .apk ao celular.
Ative "Fontes desconhecidas" no Android 2.3.6 e abra o arquivo pelo gerenciador de arquivos.
Este aplicativo não substitui o sistema, ROM ou launcher.

## Código-fonte
- src/main/java/com/ypod/ydino/GameActivity.java
- AndroidManifest.xml
- build.sh: build enxuto sem Gradle

## Compilação
O arquivo `.github/workflows/mini-game-apk.yml` produz e assina o APK com
assinatura v1, compatível com API 10. A chave de teste gerada pelo workflow é
temporária: versões futuras podem precisar de reinstalação, a menos que se
configure uma chave privada persistente.

O build não substitui o teste em hardware real.
