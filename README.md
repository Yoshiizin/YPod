# YPod Music 3.0 — Classic Edition

Player offline com interface inspirada no aplicativo **Música** dos iPods touch clássicos, redesenhado para **Samsung Galaxy Y Duos GT-S6102B**, **Android 2.3.6 (API 10)** e tela pequena de **240 × 320 px**.

## O que funciona

- Biblioteca MP3 local indexada pela MediaStore, com busca por música, artista e álbum.
- Abas **Músicas / Artistas / Álbuns / Playlists** e listas por artista e álbum.
- Player com arte do álbum quando disponível e capa alternativa quando não há imagem.
- Play/pausa, música anterior/seguinte, barra de progresso ajustável, embaralhar e repetir (desativado/tudo/uma).
- Criação, preenchimento e exclusão de playlists locais; pressione uma música para adicioná-la.
- Reprodução com wake lock para a tela desligada, controle pelo volume físico e pausa ao desconectar fones ou perder foco de áudio.
- Aplicativo independente das versões antigas do YPod, com atividade de inicialização explícita no AndroidManifest.
- Completamente offline, sem login ou serviços Google.

## Como instalar no Galaxy Y Duos

1. Baixe o ZIP da execução bem-sucedida do workflow **Build YPod APK** em **Actions**.
2. Extraia **app-debug.apk**; **não tente abrir o ZIP como APK**.
3. Transfira **app-debug.apk** para o cartão SD ou memória interna do Galaxy Y.
4. No Android 2.3, ative **Configurações → Aplicações → Fontes desconhecidas**.
5. Instale o APK. Abra **YPod Music**, adicione arquivos MP3 ao cartão SD e use **Ajustes → Atualizar músicas** se necessário.

**Importante:** a cada nova compilação em runners GitHub Actions, a assinatura de depuração pode mudar. Se você já instalou outra compilação com o MESMO identificador, o Android poderá bloquear a atualização: será necessário criar uma distribuição com assinatura permanente para upgrades sem desinstalar. Esta edição usa o identificador separado `com.pocketbeat.ypodclassic` e não substitui as instalações anteriores.

## Limitações e verificação

A compilação no GitHub Actions comprova que o APK foi produzido, **não** que foi executado no aparelho. O player funciona enquanto a Activity permanecer ativa; não possui serviço de reprodução independente com notificação de segundo plano. Este é um player Android inspirado na estética de iPod, **não** uma cópia binária do iOS nem um firmware que substitui o Android. Ajustes finos devem ser validados no GT-S6102B real.
