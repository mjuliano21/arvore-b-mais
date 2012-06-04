/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dojoarvorebmais;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

public class ArvoreBMais {

    /**
     * Executa busca em Arquivos utilizando Arvore B+ como indice
     * Assumir que ponteiro para próximo nó é igual a -1 quando não houver próximo nó
     * @param codCli: chave do cliente que está sendo buscado
     * @param nomeArquivoMetadados nome do arquivo de metadados 
     * @param nomeArquivoIndice nome do arquivo de indice (que contém os nós internos da arvore B+)
     * @param nomeArquivoDados nome do arquivo de dados (que contém as folhas da arvore B+)
     * @return uma instancia de ResultBusca, preenchida da seguinte forma:
     * Caso a chave codCli seja encontrada:
    encontrou = true
    pontFolha aponta para a página folha que contém a chave
    pos aponta para a posição em que a chave se encontra dentro da página
    
    Caso a chave codCli não seja encontrada:
    encontrou = false
    pontFolha aponta para a última página folha examinada
    pos informa a posição, nessa página, onde a chave deveria estar inserida
     */
    public ResultBusca busca(int codCli, String nomeArquivoMetadados, String nomeArquivoIndice, String nomeArquivoDados) throws Exception {
        RandomAccessFile meta = new RandomAccessFile(nomeArquivoMetadados, "rw");
        RandomAccessFile dado = new RandomAccessFile(nomeArquivoDados, "rw");
        RandomAccessFile indice = new RandomAccessFile(nomeArquivoIndice, "rw");
        int root = meta.readInt();
        boolean rf = meta.readBoolean();
        int i = 0;
        NoFolha no;
        if (rf) {
            dado.seek(root);
            no = NoFolha.le(dado);
            for (i = 0; i < no.m; i++) {
                if (codCli == no.clientes.get(i).codCliente) {
                    meta.close();
                    dado.close();
                    indice.close();
                    return new ResultBusca(root, i, true);
                }
                if (codCli < no.clientes.get(i).codCliente) {
                    meta.close();
                    dado.close();
                    indice.close();
                    return new ResultBusca(root, i, false);
                }
            }
        } else {
            indice.seek(root);
            root = buscaRec(codCli, indice);
            dado.seek(root);
            no = NoFolha.le(dado);
            for (i = 0; i < no.m; i++) {
                if (codCli == no.clientes.get(i).codCliente) {
                    meta.close();
                    dado.close();
                    indice.close();
                    return new ResultBusca(root, i, true);
                }
                if (codCli < no.clientes.get(i).codCliente) {
                    meta.close();
                    dado.close();
                    indice.close();
                    return new ResultBusca(root, i, false);
                }
            }
        }
        meta.close();
        dado.close();
        indice.close();
        return new ResultBusca(root, i, false);
    }

    public int buscaRec(int codCli, RandomAccessFile indice) throws IOException {
        NoInterno ni = NoInterno.le(indice);
        int i;
        for (i = 0; i < ni.m; i++) {
            if (codCli < ni.chaves.get(i)) {
                if (ni.apontaFolha) {
                    return ni.p.get(i);
                } else {
                    indice.seek(ni.p.get(i));
                    return buscaRec(codCli, indice);
                }
            }
            if (i + 1 == ni.m && codCli >= ni.chaves.get(i)) {
                if (ni.apontaFolha) {
                    return ni.p.get(i + 1);
                } else {
                    indice.seek(ni.p.get(i + 1));
                    return buscaRec(codCli, indice);
                }
            }
        }
        return -1;
    }

    /**
     * Executa inserção em Arquivos Indexados por Arvore B+
     * @param codCli: código do cliente a ser inserido
     * @param nomeCli: nome do Cliente a ser inserido
     * @param nomeArquivoMetadados nome do arquivo de metadados 
     * @param nomeArquivoIndice nome do arquivo de indice (que contém os nós internos da arvore B+)
     * @param nomeArquivoDados nome do arquivo de dados (que contém as folhas da arvore B+)
     * @return endereço da folha onde o cliente foi inserido, -1 se não conseguiu inserir
     */
    public int insere(int codCli, String nomeCli, String nomeArquivosMetadados, String nomeArquivoIndice, String nomeArquivoDados) throws Exception {
        RandomAccessFile dado = new RandomAccessFile(nomeArquivoDados, "rw");
        RandomAccessFile indice = new RandomAccessFile(nomeArquivoIndice, "rw");
        RandomAccessFile rafmetadados = new RandomAccessFile(nomeArquivosMetadados, "rw");
        NoFolha no;
        Cliente novo = new Cliente(codCli, nomeCli);
        ResultBusca result = busca(codCli, nomeArquivosMetadados, nomeArquivoIndice, nomeArquivoDados);
        System.out.println("0");

        if (!result.encontrou) {
            dado.seek(result.pontFolha);
            no = NoFolha.le(dado);
            System.out.println("1");

            //Passa o conteudo do nó atual uma casa para frente
            passafrente(no.clientes, result.pos);
            //adiciona o nÃ³ buscado no seu lugar correspondente
            no.clientes.add(result.pos, novo);
            //atualiza o valor de m
            no.m++;

            if (no.m < (NoFolha.d * 2) + 1) {
                dado.seek(result.pontFolha);
                no.salva(dado);
                dado.close();
                System.out.println("2");
                return result.pontFolha;

            } else {
                //Cria um novo nó
                NoFolha noNovo = new NoFolha();
                //faz inserção no arquivo de indice
                insereArquivoIndice(no.clientes.get(NoFolha.d + 1).codCliente, no.pontPai, result.pontFolha, nomeArquivosMetadados, nomeArquivoIndice);
                //Adiciona os valores a partir de d+1 no novo nó
                for (int i = NoFolha.d + 1; i < NoFolha.d * 2 + 1; i++) {
                    noNovo.clientes.add(i, no.clientes.get(i));
                    //atualiza m
                    noNovo.m++;
                }

                //Limpa as partes antes preenchidas do nÃ³ retornado pela busca
                for (int i = NoFolha.d; i < 2 * NoFolha.d + 1; i++) {
                    no.clientes.remove(i);
                    //atualiza m
                    no.m--;
                }
                //Le arquivo de MetaDados
                Metadados metadados = Metadados.le(rafmetadados);
                //faz um seek para o prÃ³ximo ponteiro livre
                dado.seek(metadados.pontProxNoFolhaLivre);
                //Atualiza os ponteiros
                noNovo.pontProx = no.pontProx;
                no.pontProx = metadados.pontProxNoFolhaLivre; //onde o novo no criado será alocado
                metadados.pontProxNoFolhaLivre += NoFolha.TAMANHO;
                //seek para o primeiro nó
                dado.seek(result.pontFolha);
                //salva o arquivo de dados
                noNovo.salva(dado);
                //seek para o segundo nó
                dado.seek(metadados.pontProxNoFolhaLivre);
                //salva o arquivo de dados
                dado.seek(result.pontFolha);
                //salva arquivo de metadados
                metadados.salva(rafmetadados);
                //fecha os arquivos
                rafmetadados.close();
                dado.close();
                indice.close();
                //retorna a folha onde o nó foi inserido
                if (result.pos >= NoFolha.d) {
                    return result.pontFolha;
                } else {
                    return metadados.pontProxNoFolhaLivre;
                }
                //return no.pontProx;
            }
        }
        return -1;
    }

    public static void passafrente(List<Cliente> cli, int pos) {
        for (int i = cli.size(); i > pos; i--) {
            cli.add(i, cli.get(i - 1));
        }
    }

    public static void passafrenteInt(List<Integer> dados, int pos) {
        for (int i = dados.size(); i > pos; i--) {
            dados.add(i, dados.get(i - 1));
        }
    }

    public void insereArquivoIndice(int codCliente, int pontPai, int pontFilho, String nomeArquivoIndice, String nomeArquivosMetadados) throws Exception {
        RandomAccessFile rafindice = new RandomAccessFile(nomeArquivoIndice, "rw");
        RandomAccessFile rafmetadados = new RandomAccessFile(nomeArquivosMetadados, "rw");
        rafindice.seek(pontPai);
        NoInterno nointerno = NoInterno.le(rafindice);
        int pos = 0;
        //procura a posição que tem que ser inserida
        for (int i = 0; i < nointerno.m; i++) {
            if (nointerno.chaves.get(i) > codCliente) {
                pos = i;
                break;
            }
        }
        //joga para frente
        passafrenteInt(nointerno.chaves, pos);
        nointerno.chaves.add(pos, codCliente);
        nointerno.m++;

        //atualiza o ponteiro
        passafrenteInt(nointerno.p, pos + 1);
        nointerno.p.add(pos + 1, pontFilho);
        //verifica se está com m > 4
        if (nointerno.m < 2 * NoInterno.d + 1) {
            rafindice.seek(pontPai);
            nointerno.salva(rafindice);
            rafindice.close();
        } else {
            //faz inserção no arquivo de indice
            insereArquivoIndice(nointerno.chaves.get(NoInterno.d + 1), nointerno.pontPai, pontPai, nomeArquivosMetadados, nomeArquivoIndice);
            //Cria um novo nó interno
            NoInterno noNovo = new NoInterno();
            //Adiciona os valores a partir de d+1 no novo nó
            for (int i = NoInterno.d + 1; i < NoInterno.d * 2 + 1; i++) {
                noNovo.chaves.add(i, nointerno.chaves.get(i));
                //atualiza m
                noNovo.m++;
            }

            //Limpa as partes antes preenchidas do nó retornado pela busca
            for (int i = NoInterno.d; i < 2 * NoInterno.d + 1; i++) {
                noNovo.chaves.remove(i);
                //atualiza m
                noNovo.m--;
            }

            //atualiza o ponteiro
            passafrenteInt(nointerno.p, pos + 1);
            nointerno.chaves.add(pos + 1, pontPai);

            //Le arquivo de MetaDados
            Metadados metadados = Metadados.le(rafmetadados);
            rafindice.seek(metadados.pontProxNoInternoLivre);

            metadados.pontProxNoFolhaLivre += NoFolha.TAMANHO;
            //salva o arquivo de dados
            noNovo.salva(rafindice);
            //fecha os arquivos
            rafmetadados.close();
            rafindice.close();

        }
    }

    /**
     * Executa exclusão em Arquivos Indexados por Arvores B+
     * @param codCli: chave do cliente a ser excluído
     * @param nomeArquivoMetadados nome do arquivo de metadados 
     * @param nomeArquivoIndice nome do arquivo de indice (que contém os nós internos da arvore B+)
     * @param nomeArquivoDados nome do arquivo de dados (que contém as folhas da arvore B+) * @return endereço do cliente que foi excluído, -1 se cliente não existe
     */
    public int exclui(int CodCli, String nomeArquivoMetadados, String nomeArquivoIndice, String nomeArquivoDados) throws Exception {
        //TODO: Inserir aqui o código do algoritmo de remoção
        RandomAccessFile rafDados = new RandomAccessFile(nomeArquivoDados, "rw");
        RandomAccessFile rafIndices = new RandomAccessFile(nomeArquivoIndice, "rw");
        ResultBusca result = busca(CodCli, nomeArquivoMetadados, nomeArquivoIndice, nomeArquivoDados);
        if (!result.encontrou) {
            return -1;
        } else {
            boolean esqFoiDeletado;
            long pontFolha = result.pontFolha;
            int pos = result.pos;
            //lendo o nó folha de onde o registro será deletado
            rafDados.seek(pontFolha);
            NoFolha noFolha = NoFolha.le(rafDados);
            long pontPai = noFolha.pontPai;
            rafIndices.seek(pontPai);
            NoInterno noPai = NoInterno.le(rafIndices);

            //achar qual dos p do pai aponta pra mim
            int i = 0;
            while (noPai.p.get(i) != pontFolha) {
                i++;
            }

            noFolha.clientes.remove(pos);
            noFolha.m = noFolha.m - 1;

            if (noFolha.m < NoFolha.d) {
                int pesq = 0;
                int pdir = 0;
                long pontEsq = 0;
                long pontDir = 0;
                NoFolha noesq = null;
                NoFolha nodir = null;

                if (i == noPai.m) {
                    pdir = i;
                    pesq = i - 1;
                    pontDir = pontFolha;
                    pontEsq = noPai.p.get(pesq);
                    rafDados.seek(pontEsq);
                    noesq = NoFolha.le(rafDados);
                    nodir = noFolha;
                    //neste caso o nó a direita que foi excluido
                    esqFoiDeletado = false;
                } else {
                    if (i == 0) {
                        pdir = i + 1;
                        pesq = i;
                        pontEsq = pontFolha;
                        pontDir = noPai.p.get(pdir);
                        rafDados.seek(pontDir);
                        nodir = NoFolha.le(rafDados);
                        noesq = noFolha;
                        //neste caso foi o nó esquerdo que foi excluido
                        esqFoiDeletado = true;
                    } else {
                        pdir = i + 1;
                        pesq = i;
                        pontEsq = pontFolha;
                        pontDir = noPai.p.get(pdir);
                        rafDados.seek(pontDir);
                        nodir = NoFolha.le(rafDados);
                        noesq = noFolha;
                        //neste caso foi o nó esquerdo que foi excluido
                        esqFoiDeletado = true;
                        if ((nodir.m + noesq.m) >= (NoFolha.d * 2)) {
                            pdir = i;
                            pesq = i - 1;
                            pontDir = pontFolha;
                            pontEsq = noPai.p.get(pesq);
                            rafDados.seek(pontEsq);
                            noesq = NoFolha.le(rafDados);
                            nodir = noFolha;
                            //neste caso foi o nó direito que foi excluido
                            esqFoiDeletado = false;
                        }
                    }
                }
                if ((nodir.m + noesq.m) < (NoFolha.d * 2)) {
                    //concateno
                    for (int j = 0; j < nodir.m; j++) {
                        noesq.clientes.add(nodir.clientes.get(j));
                    }
                    noesq.m = noesq.m + nodir.m;
                    nodir.m = 0;
                    noesq.pontProx = nodir.pontProx;
                    rafDados.seek(pontEsq);
                    noesq.salva(rafDados);
                    rafDados.seek(pontDir);
                    nodir.salva(rafDados);

                    noPai.p.remove(pdir);
                    noPai.p.add(-1);
                    noPai.chaves.remove(pesq);
                    noPai.m = noPai.m - 1;

                    rafIndices.seek(pontPai);
                    noPai.salva(rafIndices);
                    if ((noPai.m < NoInterno.d) && noPai.pontPai != -1) {
                        tratarDados(rafIndices, nomeArquivoMetadados, pontPai);
                    } else {
                    }

                } else {
                    //redistribuo, quando for ímpar o da direita fica com mais, seguindo o teste
                    int qntTotal = 0;
                    int qntEsq = 0;
                    int qntDir = 0;
                    //Fica com menos o nó que teve um cliente removido
                    if (esqFoiDeletado) {
                        qntTotal = noesq.m + nodir.m;
                        qntEsq = qntTotal / 2;
                        qntDir = qntTotal - qntEsq;
                    } else {
                        qntTotal = noesq.m + nodir.m;
                        qntDir = qntTotal / 2;
                        qntEsq = qntTotal - qntDir;
                    }

                    //saber se a esquerda precisa de clientes da direita
                    if (noesq.m < qntEsq) {

                        while (noesq.m < qntEsq) {
                            Cliente c = nodir.clientes.get(0);
                            nodir.clientes.remove(0);
                            noesq.clientes.add(c);
                            nodir.m = nodir.m - 1;
                            noesq.m = noesq.m + 1;
                        }
                    } else {
                        //senão direita precisa de clientes da esquerda
                        while (nodir.m < qntDir) {
                            Cliente c = noesq.clientes.get(noesq.m - 1);
                            noesq.clientes.remove(noesq.m - 1);
                            nodir.clientes.add(0, c);
                            nodir.m = nodir.m + 1;
                            noesq.m = noesq.m - 1;
                        }
                    }

                    noPai.chaves.set(pesq, nodir.clientes.get(0).codCliente);
                    rafIndices.seek(pontPai);
                    noPai.salva(rafIndices);
                    rafDados.seek(pontEsq);
                    noesq.salva(rafDados);
                    rafDados.seek(pontDir);
                    nodir.salva(rafDados);
                }
            } else {
                rafDados.seek(pontFolha);
                noFolha.salva(rafDados);
            }
        }
        rafDados.close();
        rafIndices.close();
        return result.pontFolha;
    }

    public void tratarDados(RandomAccessFile rafIndices, String nomeArquivoMetadados, long pont) throws Exception {
        rafIndices.seek(pont);
        NoInterno no = NoInterno.le(rafIndices);
        long pontPai = no.pontPai;
        rafIndices.seek(pontPai);
        NoInterno noPai = NoInterno.le(rafIndices);

        //achar qual dos p do pai aponta pra mim
        int i = 0;
        while (noPai.p.get(i) != pont) {
            i++;
        }

        if (no.m < NoInterno.d) {
            int pesq = 0;
            int pdir = 0;
            long pontEsq = 0;
            long pontDir = 0;
            NoInterno noesq = null;
            NoInterno nodir = null;

            if (i == noPai.m) {
                pdir = i;
                pesq = i - 1;
                pontDir = pont;
                pontEsq = noPai.p.get(pesq);
                rafIndices.seek(pontEsq);
                noesq = NoInterno.le(rafIndices);
                nodir = no;
            } else {
                if (i == 0) {
                    pdir = i + 1;
                    pesq = i;
                    pontEsq = pont;
                    pontDir = noPai.p.get(pdir);
                    rafIndices.seek(pontDir);
                    nodir = NoInterno.le(rafIndices);
                    noesq = no;
                } else {
                    pdir = i + 1;
                    pesq = i;
                    pontEsq = pont;
                    pontDir = noPai.p.get(pdir);
                    rafIndices.seek(pontDir);
                    nodir = NoInterno.le(rafIndices);
                    noesq = no;
                    if ((nodir.m + noesq.m) >= NoFolha.d) {
                        pdir = i;
                        pesq = i - 1;
                        pontDir = pont;
                        pontEsq = noPai.p.get(pesq);
                        rafIndices.seek(pontEsq);
                        noesq = NoInterno.le(rafIndices);
                        nodir = no;
                    }
                }
            }

            if ((nodir.m + noesq.m) < NoFolha.d) {
                //concateno
                for (int j = 0; j < nodir.m; j++) {
                    noesq.chaves.add(nodir.chaves.get(j));
                }
                nodir.m = 0;
                rafIndices.seek(pontEsq);
                noesq.salva(rafIndices);
                rafIndices.seek(pontDir);
                nodir.salva(rafIndices);

                noPai.p.remove(pdir);
                noPai.p.add(-1);
                noPai.chaves.remove(pesq);
                noPai.m = noPai.m - 1;

                rafIndices.seek(pontPai);
                noPai.salva(rafIndices);
                if (noPai.m == 0) {
                    RandomAccessFile rafMeta = new RandomAccessFile(nomeArquivoMetadados, "rw");
                    rafMeta.seek(0);
                    Metadados meta = Metadados.le(rafMeta);
                    meta.pontRaiz = (int) pontEsq;
                    rafMeta.seek(0);
                    meta.salva(rafMeta);
                    rafMeta.close();
                } else {
                    if ((noPai.m < NoInterno.d) && noPai.pontPai != -1) {
                        tratarDados(rafIndices, nomeArquivoMetadados, pontPai);
                    } else {
                    }
                }
            } else {
                //redistribuo
            }
        } else {
            rafIndices.seek(pont);
            no.salva(rafIndices);
        }
    }
}
