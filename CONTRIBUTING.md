# Contribuindo com o YPod

Obrigado por ajudar a manter o projeto vivo!

O foco do YPod é funcionar em **Android 2.3.6 (API 10)** no **GT-S6102B**, respeitando a tela de **240 × 320 px**, memória limitada e uso offline.

## Bugs e melhorias

1. Consulte as [Issues existentes](https://github.com/Yoshiizin/YPod/issues).
2. Abra uma nova issue informando o app, sua versão, o modelo/Android, passos para reproduzir e resultado esperado.
3. Sempre que possível, envie uma captura de tela **sem notificações, contas, contatos ou outros dados pessoais**.
4. Em caso de erro de instalação, mencione se já havia outra versão instalada (a chave de assinatura pode ter mudado).

## Pull requests

- Faça mudanças pequenas, documentadas e específicas por app.
- Não aumente o `minSdkVersion` acima de **10**.
- Evite bibliotecas modernas obrigatórias, serviços externos e assets pesados.
- Atualize a seção correspondente do README quando alterar a interface.
- Confira o build relevante em `Actions`.
- Não inclua `.jks`, `.keystore`, tokens nem outras credenciais.

## Telas e imagens

Os arquivos SVG em `docs/showcase/` são **prévias ilustrativas**. Capturas reais de um Galaxy Y podem ser acrescentadas depois do teste, com indicação clara de qual APK foi usado. Não substitua previews por renderizações apresentadas como capturas autênticas.

## Licenciamento

O repositório é público, mas isso não significa automaticamente que o conteúdo tenha uma licença de reutilização. Antes de redistribuir partes ou publicar um fork sob uma licença, verifique os direitos aplicáveis e a licença que o mantenedor decidir adotar.
