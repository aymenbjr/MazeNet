package de.fhac.MazeNet;

 
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.xml.bind.UnmarshalException;

import de.fhac.mazenet.server.game.Board;
import de.fhac.mazenet.server.game.Position;
import de.fhac.mazenet.server.generated.AwaitMoveMessageData;
import de.fhac.mazenet.server.generated.BoardData;
import de.fhac.mazenet.server.generated.CardData;
import de.fhac.mazenet.server.generated.MazeCom;
import de.fhac.mazenet.server.generated.MazeComMessagetype;
import de.fhac.mazenet.server.generated.MoveMessageData;
import de.fhac.mazenet.server.generated.PositionData;
import de.fhac.mazenet.server.generated.Treasure;
import de.fhac.mazenet.server.networking.MazeComMessageFactory;
import de.fhac.mazenet.server.networking.XmlInputStream;
import de.fhac.mazenet.server.networking.XmlOutputStream;  

public class Client {
	
//	final static int port = 5123;
//	final static String username = "Aymen";
//	final static String ip = "127.0.0.1";
	 static Client client = new Client();
	 String ip, username;
	 Socket cSocket;
	 XmlInputStream receiver;	static XmlOutputStream sender;
	 Random rnd = new Random();
	 PositionData spielerPositionData = new PositionData();
	 PositionData schatzPositionData = new PositionData();
	 PositionData pinPosition = new PositionData();
	 PositionData shiftPosition = new PositionData();
	 MazeComMessagetype msgType;
	 boolean stop = false;
	 int port, id, index;
	 Position shiftCard = new Position();
	 Position randCardPosition = new Position();
	 MazeCom mazeCom = new MazeCom();
	 MazeCom maze = new MazeCom();
	 List<Position> possibePositions, reachablePositions;
	 BoardData boardData;
	 Board board;
	 CardData card;
	 Treasure treasure;
	 AwaitMoveMessageData awaiMoveMsg;
	 MoveMessageData moveMsg = new MoveMessageData();
	 boolean rand;
	 FileInputStream config;
	 Properties properties = new Properties();


	public static void main(final String[] args) throws IOException, UnmarshalException {
		client.configProjekt();
	}

	public void configProjekt() throws UnmarshalException, IOException {
		
		config = new FileInputStream("src/main/resources/Config-Datei");
		properties.load(config);
		ip = properties.getProperty("ip");
		username = properties.getProperty("username");
		port = Integer.parseInt(properties.getProperty("port"));
		mazeCom = MazeComMessageFactory.createLoginMessage(username);
		System.out.println("Welcome "+mazeCom.getLoginMessage().getName());
		System.out.println("0.0 "+mazeCom.getMessagetype());
		cSocket = new Socket(ip, port);
		receiver = new XmlInputStream(cSocket.getInputStream());
		sender = new XmlOutputStream(cSocket.getOutputStream());
		//------------------------- WHY
		sender.write(mazeCom);
		System.out.println("0 "+mazeCom.getMessagetype());
		mazeCom = receiver.readMazeCom();
		System.out.println("1 "+mazeCom.getMessagetype());
		id = mazeCom.getId();
		//-------------------------
		while (!stop) {
			//System.out.println("1.5 "+mazeCom.getMessagetype());
			configMessages();
			}
	}
	
	public void configMessages() throws UnmarshalException, IOException {

		mazeCom = receiver.readMazeCom(); //KIDRNA SETTINA AWAITMOVEMESSSAGE?
		System.out.println("2 "+mazeCom.getMessagetype());
		msgType= mazeCom.getMessagetype();
		
		switch (msgType) {
		//------------------------- KITAYDIRO YTGENERAW HAD LMESAGET? MNIN KIJIW?
		case AWAITMOVE: //------------------------- CHHAD AWAITMOVE 3AWD?
			awaiMoveMsg = mazeCom.getAwaitMoveMessage();
			boardData = awaiMoveMsg.getBoard();
			board = new Board(boardData);
			card = boardData.getShiftCard(); // LACH MAJBNACH SHIFTCARD MN L BOARD (ATTRIBUT FIHA)
			treasure = awaiMoveMsg.getTreasureToFindNext();
			freePaths(maze);
			break;

		case WIN:
			stop = true;
			cSocket.close();
			break;

		case DISCONNECT:
			stop = true;
			cSocket.close();
			break;

		default:
			break;
		}
	

	}

	public void freePaths(MazeCom mazeCom) throws UnmarshalException, IOException {
		
		possibePositions = de.fhac.mazenet.server.game.Position.getPossiblePositionsForShiftcard();
		shiftCard.setRow(shiftPosition.getRow()); // MNIN JABT shiftPosition VALEUR DYALHA
		shiftCard.setCol(shiftPosition.getCol());
		possibePositions.remove(shiftCard.getOpposite());

		etq: for (Position possiblePosition : possibePositions) {
			rand = true;
			shiftPosition.setRow(possiblePosition.getRow());
			shiftPosition.setCol(possiblePosition.getCol());

			moveMsg.setShiftPosition(shiftPosition);
			moveMsg.setShiftCard(card);

			Board fakeboard = board.fakeShift(moveMsg);

			schatzPositionData = fakeboard.findTreasure(treasure);
			spielerPositionData = fakeboard.findPlayer(id);
			if (schatzPositionData != null) { //if the schatzPositionData card is not the shiftcard
//ki
				reachablePositions = fakeboard.getAllReachablePositions(spielerPositionData);
				System.out.println(reachablePositions);
				for (Position position : reachablePositions) {

					if (schatzPositionData.getRow() == position.getRow() && schatzPositionData.getCol() == position.getCol()) {
						pinPosition.setRow(schatzPositionData.getRow());
						pinPosition.setCol(schatzPositionData.getCol());
						moveMsg.setNewPinPos(pinPosition);
						rand = false;
						break etq;
					}
				}

			}
		}

	if (rand){
		randomPush(possibePositions);
	}
		
		mazeCom.setMessagetype(MazeComMessagetype.MOVE);
		mazeCom.setMoveMessage(moveMsg);
		mazeCom.setId(id);
		sender.write(mazeCom);
		System.out.println("3 "+mazeCom.getMessagetype());

	}

	public void randomPush(List<Position> possibePositions) throws UnmarshalException, IOException {
		
		index = rnd.nextInt(possibePositions.size());
		randCardPosition = possibePositions.get(index);
// 12 --> 11

		shiftPosition.setRow(randCardPosition.getRow());
		shiftPosition.setCol(randCardPosition.getCol());
		moveMsg.setShiftPosition(shiftPosition);
		moveMsg.setShiftCard(card);
		
		Board fakeboard = board.fakeShift(moveMsg);
		spielerPositionData = fakeboard.findPlayer(id);
		moveMsg.setNewPinPos(spielerPositionData);

	}
}
/**
 * 															||DICTIONARY||
 * 			shiftPosition: position li ghatkhcha fiha lcarta
 * 			card: 
 * 			shiftCard: 
 * 			moveMsg: Data dlMazeCom li fiha les donnees kamlin(shiftPosition, position dl pin, card )
 *         ----        	Kan3mro les donnees li ghan7tajo bach l3bo (fin nkhchiw lcarta, fin n7rko l pin, o **** )
 *          					f objet de type MazeCom o kansiftoh l server (.write) bach ytdar          			  ----
 *          readMazeCom: kat9ra lmessagat de type MazeCom
 * 
 */
