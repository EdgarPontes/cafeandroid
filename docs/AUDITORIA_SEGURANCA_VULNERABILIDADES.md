# Relatório de Auditoria: Vulnerabilidades e Riscos de Segurança

Este documento detalha as falhas de segurança identificadas no projeto **CafeSG** após auditoria técnica realizada em Abril de 2026. Estas vulnerabilidades devem ser tratadas em fases futuras de desenvolvimento para garantir a conformidade com a LGPD e a integridade dos dados.

---

## 🚨 Vulnerabilidades Críticas Identificadas

### 1. Comunicação Insegura (HTTP)
- **Localização**: `AndroidManifest.xml` (`android:usesCleartextTraffic="true"`)
- **Vulnerabilidade**: O aplicativo permite tráfego de rede em texto simples (sem criptografia SSL/TLS).
- **Risco**: Ataques de *Man-in-the-Middle* (MitM). Um atacante na mesma rede Wi-Fi pode interceptar e ler nomes de funcionários, códigos de registro e fotos em tempo real.
- **Impacto**: Alto (Vazamento de dados em trânsito).

### 2. Ausência de Autenticação/Autorização na API
- **Localização**: `CafeApi.kt`
- **Vulnerabilidade**: Nenhum endpoint da API exige cabeçalhos de autorização (`API Key`, `Bearer Token`, etc.).
- **Risco**: Qualquer dispositivo que conheça o endereço do servidor pode realizar requisições POST para registrar consumos falsos ou requisições GET para baixar a base completa de funcionários.
- **Impacto**: Crítico (Injeção de dados falsos e acesso não autorizado).

### 3. Exposição de Dados Pessoais (LGPD)
- **Localização**: `CafeApi.kt` (Endpoint `/api/funcionarios`)
- **Vulnerabilidade**: O endpoint retorna a lista completa de colaboradores sem filtros ou restrições de acesso.
- **Risco**: Raspagem de dados (*data scraping*) de todos os funcionários da empresa.
- **Impacto**: Crítico (Não conformidade legal e risco de privacidade).

### 4. Armazenamento Local sem Criptografia
- **Localização**: `AppDatabase.kt` (Room Database)
- **Vulnerabilidade**: Dados de funcionários, registros de consumo pendentes e fotos em Base64 são salvos em um banco de dados SQLite comum no armazenamento do app.
- **Risco**: Em dispositivos com acesso *root* ou se o dispositivo for fisicamente comprometido, a base de dados pode ser extraída e lida facilmente.
- **Impacto**: Médio (Exposição de dados em caso de perda do dispositivo).

### 5. Superfície de Ataque por Redirecionamento de Servidor
- **Localização**: `MainActivity.kt` / `IpConfigDialog`
- **Vulnerabilidade**: O endereço do servidor (IP/URL) é configurável via interface sem validação de certificado ou sanitização rigorosa.
- **Risco**: Um usuário pode apontar o app para um servidor malicioso ("Phishing de API") que imita o comportamento do servidor real para capturar dados de funcionários e fotos.
- **Impacto**: Médio (Redirecionamento de tráfego).

---

## 🛠️ Plano de Mitigação Recomendado (Roadmap de Segurança)

| Prioridade | Ação de Mitigação | Descrição Técnica |
| :--- | :--- | :--- |
| **P0 (Imediata)** | **Impor HTTPS** | Desativar `usesCleartextTraffic` e configurar `network_security_config.xml`. |
| **P0 (Imediata)** | **API Handshake** | Implementar `Authorization: Bearer <TOKEN>` em todas as chamadas Retrofit. |
| **P1 (Alta)** | **Criptografia Local** | Integrar **SQLCipher** ao Room para criptografar o banco `cafe_sg_database`. |
| **P1 (Alta)** | **Sanitização de IP** | Validar formato de IP/URL e implementar *Certificate Pinning* se possível. |
| **P2 (Média)** | **Obfuscação de Código** | Configurar o **R8/ProGuard** para dificultar a engenharia reversa das chaves de API. |

---

**Nota**: Estas melhorias devem ser implementadas em conjunto com atualizações no servidor (Backend), que também deve passar a exigir autenticação para os referidos endpoints.
