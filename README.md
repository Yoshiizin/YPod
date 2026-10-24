# YPod — Galaxy Y Duos Retro Collection 🦖

Apps leves, offline e com visual retrô para o **Samsung Galaxy Y Duos GT-S6102B** (Android **2.3.6 / API 10**, tela **240 × 320**).

## Projetos

| App | Versão | O que faz | Código | APK / build |
| --- | --- | --- | --- | --- |
| **YPod Music Classic** | 3.0 | Player MP3, playlists, artistas, álbuns e reprodução offline | [`app/`](./app) | [Build YPod APK](https://github.com/Yoshiizin/YPod/actions/workflows/build-apk.yml) |
| **YPod Home Classic** | 1.0 | Launcher clássico que já existia neste repositório | [`home/`](./home) | [Build YPod APK](https://github.com/Yoshiizin/YPod/actions/workflows/build-apk.yml) |
| **YPod Mini Home Dynamic** | **4.1** | Launcher azul estilo mini-home BALMUDA, com relógio digital, data, bateria, gestos, ícones vetoriais e correção da área de toque do **Music** | [`mini-home/`](./mini-home) | [Build Mini Home Galaxy Y](https://github.com/Yoshiizin/YPod/actions/workflows/mini-home-apk.yml) |
| **Y-Dino** | **1.0** | Mini game arcade de dinossauro: tocar para pular, obstáculos, pontos, recorde e som opcional | [`mini-game/`](./mini-game) | [Build Y-Dino for Galaxy Y](https://github.com/Yoshiizin/YPod/actions/workflows/mini-game-apk.yml) |

> Os quatro são **aplicativos Android independentes**. Mini Home e Y-Dino **não** são ROMs e não exigem root. O projeto de ROM/YPod OS é uma iniciativa de desenvolvimento separada: não tente instalar os ZIPs de código como firmware.

## Instalar no Galaxy Y (sem terminal)

1. Abra o link de **APK / build** na tabela acima e entre na execução mais recente com **✓ Success**.
2. Role até **Artifacts**, baixe o ZIP do aplicativo e **extraia o arquivo `.apk`** (não instale o ZIP).
3. Copie o APK para o cartão SD ou para o telefone.
4. No Galaxy Y: **Configurações → Aplicativos → Fontes desconhecidas**; depois abra o APK em Arquivos.
5. Para usar a Mini Home, aperte o botão físico **Home** e selecione **YPod Mini Home 4.1**.
6. Na Mini Home, **segure o ícone Music** e escolha seu player YPod. O **Y-Dino** aparece na lista de aplicativos.

**Atenção às assinaturas:** os workflows usam uma chave de desenvolvimento temporária criada durante cada execução. APKs recompilados podem ter assinaturas diferentes e **não atualizar** uma instalação com o mesmo pacote. Antes de desinstalar, salve o que for importante (por exemplo, recordes do Y-Dino). Para permitir futuras atualizações preservando os dados, será necessário configurar uma chave de assinatura persistente e privada.

## Builds no GitHub Actions

- [YPod Music + Home Classic](https://github.com/Yoshiizin/YPod/actions/workflows/build-apk.yml) — gera `app-debug.apk` e `home-debug.apk`.
- [Mini Home v4.1](https://github.com/Yoshiizin/YPod/actions/workflows/mini-home-apk.yml) — gera `YPod-Mini-Home-Galaxy-Y.apk`.
- [Y-Dino](https://github.com/Yoshiizin/YPod/actions/workflows/mini-game-apk.yml) — gera `YPod-Y-Dino-Galaxy-Y.apk`.

Os dois workflows novos podem ser executados por **Actions → workflow desejado → Run workflow**; também são acionados por alterações nos respectivos projetos na branch `main`.

Compilar com sucesso **não** garante funcionamento perfeito no hardware real. Teste instalação, som, desempenho, toque e orientação no **GT-S6102B** antes de substituir sua configuração atual.

---

## Detalhes do player musical

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
