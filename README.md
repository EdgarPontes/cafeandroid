# Cafe SG

Projeto Android desenvolvido para gestão/consumo de serviços de cafeteria.

## 🚀 Tecnologias Utilizadas

- **Linguagem:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Rede:** Retrofit & Gson
- **Persistência:** Room Database
- **Imagens:** Coil
- **Arquitetura:** MVVM (Model-View-ViewModel)

## ⚙️ Configurações do Projeto

- **Package Name:** `br.com.sgsistemas.cafesg`
- **SDK Mínimo:** 31 (Android 12)
- **SDK Alvo:** 36
- **Versão:** 1.0

## 📦 Como Gerar o APK

### 1. Pela Interface do Android Studio (Recomendado)

1. No menu superior, clique em **Build**.
2. Vá em **Build Bundle(s) / APK(s)**.
3. Clique em **Build APK(s)**.
4. O Android Studio iniciará o processo. Quando terminar, aparecerá um balão de notificação no canto inferior direito. Clique em **locate** para abrir a pasta onde o arquivo `.apk` foi gerado.

### 2. Pela Linha de Comando (Terminal)

Se preferir usar o terminal dentro do Android Studio:

*   **APK de Debug** (uso interno/testes):
    ```bash
    ./gradlew assembleDebug
    ```
    O arquivo será gerado em: `app/build/outputs/apk/debug/app-debug.apk`

*   **APK de Release** (produção):
    ```bash
    ./gradlew assembleRelease
    ```
    *Nota: Para o APK de release, você precisará ter configurado a assinatura do app (signing configs) no seu arquivo `build.gradle`.*

### 3. Gerando um APK Assinado para a Google Play

Se o objetivo for publicar ou enviar uma versão final oficial:
1. Vá em **Build** > **Generate Signed Bundle / APK...**
2. Selecione **APK** e clique em **Next**.
3. Siga as instruções para criar ou selecionar sua chave de assinatura (`Key store path`).
4. Escolha o tipo de build (geralmente `release`) e clique em **Finish**.
