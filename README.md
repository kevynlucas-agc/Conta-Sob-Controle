# Conta Sob Controle — versão de desenvolvimento 1.1

Aplicativo Android offline-first para organização de despesas domésticas. Projeto em Kotlin, Jetpack Compose, Material 3, MVVM, Room, DataStore, Hilt e Navigation Compose.

## Executar
1. Instale Android Studio compatível com Android Gradle Plugin 8.7.3 e JDK 17.
2. Abra a pasta `ContaSobControle` no Android Studio.
3. Configure o Android SDK 35 quando solicitado.
4. Sincronize o Gradle e execute a configuração `app` em um emulador ou aparelho Android 8.0+.

O projeto não inclui Gradle Wrapper. Caso o Android Studio solicite, selecione uma instalação Gradle 8.9 ou gere o wrapper pelo próprio Android Studio/terminal com `gradle wrapper --gradle-version 8.9`.

## Funcionalidades integradas
- Dashboard com totais do mês, valores pagos/pendentes, orçamento e comparação com o mês anterior.
- Cadastro de despesas por categoria, edição, exclusão com confirmação e baixa como paga.
- Persistência Room local; orçamento, renda e tema em DataStore.
- Despesas recorrentes mensais: geração idempotente da ocorrência do mês atual na inicialização, evitando duplicação por nome/categoria/mês.
- Lembretes locais de vencimento via AlarmManager/ BroadcastReceiver; lembrete na véspera às 9h (ou próximo minuto quando o vencimento é hoje). Reprogramação após reinicialização do aparelho. Requer permissão de notificações no Android 13+.
- Calendário mensal com indicação de vencimentos, pagamentos, vencidas e vencimento de hoje.
- Leituras de água e energia, histórico e diferença entre duas leituras consecutivas.
- Relatórios por dia, semana, mês, ano e categoria, mais gráfico de evolução dos últimos seis meses.
- Planejamento de renda e orçamento mensal.
- Simulador de custo de morar sozinho com valores informados pelo usuário (campos iniciam vazios).
- Backup JSON via seletor de documentos, incluindo despesas, leituras, renda, orçamento e tema; restauração validada e com confirmação antes de substituir os dados.
- Tema sistema/claro/escuro persistido.
- Testes unitários básicos de aritmética financeira.

## Armazenamento e precisão
- Nenhuma conta de demonstração é criada.
- Valores monetários persistidos em centavos (`Long`) e percentuais calculados com `BigDecimal`.
- Room e DataStore usam o sandbox privado do aplicativo. O arquivo JSON exportado é legível; armazene-o em local protegido se contiver informações pessoais.
- Nenhuma funcionalidade de negócio depende de conexão com internet.

## Limitações conhecidas / validação antes da publicação
- A compilação Android/Gradle e testes instrumentados precisam ser executados em ambiente com Android SDK e Gradle; esse ambiente de geração não possui Android SDK nem Gradle instalado. Foi feita verificação estática de sintaxe Kotlin, XML e testes isolados dos cálculos, mas isso não substitui `assembleDebug` e testes em dispositivo.
- O calendário é uma visualização mensal de vencimentos; não oferece edição por toque no dia.
- A recorrência cria a ocorrência do mês atual ao iniciar o app, não um agendador em segundo plano contínuo.
- O lembrete é local e único por vencimento; o usuário precisa autorizar notificações. Alarmes podem sofrer restrições de economia de bateria do fabricante.
- Leituras registram valores reais digitados pelo usuário. A diferença entre leituras consecutivas é consumo do intervalo; o app não inventa tarifas nem estima conta sem preço por unidade configurado.
- O backup JSON não é criptografado. O banco local fica protegido pelo sandbox Android, mas o usuário deve proteger o arquivo exportado.
- Antes de publicar: executar build e testes, revisar permissões, política de privacidade, acessibilidade, ícone, screenshots, migrações Room e compatibilidade em aparelhos reais.

## Estrutura
- `data`: Room, DAO, entidades e repositório/DataStore
- `domain`: cálculos financeiros
- `presentation`: ViewModel, tema, navegação e telas Compose
- `notifications`: alarmes e receivers locais
- `di`: configuração Hilt
