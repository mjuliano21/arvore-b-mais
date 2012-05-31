/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dojoarvorebmais;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

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
        int ptinter = meta.readInt();
        int ptleaf = meta.readInt();
        int i = 0;
        NoFolha no;
        NoInterno ni;
        if (rf) {
            dado.seek(root);
            no = NoFolha.le(dado);
            for (i = 0; i < no.m; i++) {
                if (codCli == no.clientes.get(i).codCliente) {
                    return new ResultBusca(root, i, true);
                }
                if (codCli<no.clientes.get(i).codCliente){
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
                    return new ResultBusca(root, i, true);
                }
                if (codCli<no.clientes.get(i).codCliente){
                    return new ResultBusca(root, i, false);
                }
            }
        }
        return new ResultBusca(root, i, false);
    }

    public int buscaRec(int codCli, RandomAccessFile indice) throws IOException {
        NoInterno ni = NoInterno.le(indice);
        int i;
        for ( i = 0; i < ni.m; i++) {
            if (codCli < ni.chaves.get(i)){
                if (ni.apontaFolha){
                    return ni.p.get(i);
                } else {
                    indice.seek(ni.p.get(i));
                    return buscaRec(codCli,indice);
                }
            }
            if (i+1 == ni.m && codCli >= ni.chaves.get(i)){
                if (ni.apontaFolha){
                    return ni.p.get(i+1);
                } else {
                    indice.seek(ni.p.get(i+1));
                    return buscaRec(codCli,indice);
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
     * @param nomeArquivoDados nome do arquivo de dados (que contém as folhas da arvore B+)* @return endereço da folha onde o cliente foi inserido, -1 se não conseguiu inserir
     */
    public int insere(int codCli, String nomeCli, String nomeArquivosMetadados, String nomeArquivoIndice, String nomeArquivoDados) throws Exception {
        RandomAccessFile dado = new RandomAccessFile(nomeArquivoDados, "rw");
        int i = 0;
        NoFolha no;
        Cliente novo = new Cliente(codCli,nomeCli);
        ResultBusca result = busca(codCli,nomeArquivosMetadados,nomeArquivoIndice,nomeArquivoDados);
        if (!result.encontrou){
            dado.seek(result.pontFolha);
            no = NoFolha.le(dado);
            
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Executa exclusão em Arquivos Indexados por Arvores B+
     * @param codCli: chave do cliente a ser excluído
     * @param nomeArquivoMetadados nome do arquivo de metadados 
     * @param nomeArquivoIndice nome do arquivo de indice (que contém os nós internos da arvore B+)
     * @param nomeArquivoDados nome do arquivo de dados (que contém as folhas da arvore B+) * @return endereço do cliente que foi excluído, -1 se cliente não existe
     */
    public int exclui(int CodCli, String nomeArquivoMetadados, String nomeArquivoIndice, String nomeArquivoDados) {
        //TODO: Inserir aqui o código do algoritmo de remoção
        return Integer.MAX_VALUE;
    }
}
