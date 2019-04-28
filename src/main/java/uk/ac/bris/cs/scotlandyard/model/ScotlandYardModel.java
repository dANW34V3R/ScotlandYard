package uk.ac.bris.cs.scotlandyard.model;

import java.util.*;
import java.util.function.Consumer;

import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;

import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move> {

	List<Boolean> rounds;
	Graph<Integer, Transport> graph;
	List<ScotlandYardPlayer> players;
	private int currentPlayerIndex = 0;
	private int currentRound = 0;
	private int rotation = 0;
	private boolean isGameOver = false;
	int mrXLastLocation = 0;
	List<Spectator> spectators = new ArrayList<>();
	List<ScotlandYardPlayer> detectives;


	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
							 PlayerConfiguration mrX, PlayerConfiguration firstDetective,
							 PlayerConfiguration... restOfTheDetectives) {

		this.rounds = requireNonNull(rounds);
		this.graph = requireNonNull(graph);
		players = new ArrayList<>();


		if (rounds.isEmpty()) {
			throw new IllegalArgumentException("Empty rounds");
		}

		if (graph.isEmpty()) {
			throw new IllegalArgumentException("Empty graph");
		}

		if (mrX.colour != BLACK) { // or mr.colour.isDetective()
			throw new IllegalArgumentException("MrX should be Black");
		}

		ArrayList<PlayerConfiguration> configurations = new ArrayList<>();
		for (PlayerConfiguration configuration : restOfTheDetectives)
			configurations.add(requireNonNull(configuration));
		configurations.add(0, firstDetective);
		configurations.add(0, mrX);

		Set<Integer> set = new HashSet<>();
		Set<Colour> setColour = new HashSet<>();

		for (PlayerConfiguration configuration : configurations) {
			if (set.contains(configuration.location))
				throw new IllegalArgumentException("Duplicate location");
			set.add(configuration.location);

			if (setColour.contains(configuration.colour))
				throw new IllegalArgumentException("Duplicate colour");
			setColour.add(configuration.colour);

			if (!configuration.equals(mrX)) {
				if (!(configuration.tickets.containsKey(Ticket.valueOf("TAXI")) && configuration.tickets.containsKey(Ticket.valueOf("BUS")) && configuration.tickets.containsKey(Ticket.valueOf("UNDERGROUND")) && configuration.tickets.containsKey(Ticket.valueOf("SECRET")) && configuration.tickets.containsKey(Ticket.valueOf("DOUBLE"))))
					throw new IllegalArgumentException("Detectives don't contain correct tickets");
				if (!(configuration.tickets.get(Ticket.valueOf("SECRET")) == 0 && configuration.tickets.get(Ticket.valueOf("DOUBLE")) == 0))
					throw new IllegalArgumentException("Detectives contain SECRET or DOUBLE ticket");
			} else {
				if (!(configuration.tickets.containsKey(Ticket.valueOf("TAXI")) && configuration.tickets.containsKey(Ticket.valueOf("BUS")) && configuration.tickets.containsKey(Ticket.valueOf("UNDERGROUND")) && configuration.tickets.containsKey(Ticket.valueOf("DOUBLE")) && configuration.tickets.containsKey(Ticket.valueOf("SECRET"))))
					throw new IllegalArgumentException("MrX doesn't contain correct tickets");
			}


			players.add(new ScotlandYardPlayer(configuration.player, configuration.colour, configuration.location, configuration.tickets));

			detectives = new ArrayList<>();
			for (ScotlandYardPlayer p : players) {
				if (p.isDetective()) detectives.add(p);
			}
		}
	}

	@Override
	public void registerSpectator(Spectator spectator) {						//Adds specific spectator to the spectator set
		if (spectator == null) {                                                //while checking that spectator is not null and is not currently contained within the set
			throw new NullPointerException("Spectators reference is null therefore cannot be added");
		}
		else if (getSpectators().contains(spectator)) {
			throw new IllegalArgumentException("Spectator is already added");
		}
		else {
			spectators.add(spectator);
		}
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {								//Removes specific spectator from the spectator set
		if (spectator == null) {														//while checking that spectator is not null and is currently contained within the set
			throw new NullPointerException("Spectators reference is null therefore cannot be removed");
		}
		else if (!getSpectators().contains(spectator)) {
			throw new IllegalArgumentException("Spectator is not present therefore cannot be removed");
		}
		else {
			spectators.remove(spectator);
		}

	}

	@Override
	public void accept(Move move) {
		System.out.println("test ----" + move.toString());

//		if (move == null) {
//			throw new NullPointerException("Move cannot be null");
//		}


		ScotlandYardPlayer currentPlayer = players.get(currentPlayerIndex);


		if (!(validMoves(currentPlayer).contains(move))){
			System.out.println("test throws----" + move.toString());
			throw new IllegalArgumentException("Move is not valid");       //Throws an exception if the move chosen is not part of the valid moves
		}


		currentPlayerIndex = (currentPlayerIndex + 1) % players.size();    //Increments the player index. Modulus allows counter to loop back to 0(Mr X) after all detectives have moved

		if(currentPlayer.colour() == BLACK){							   //The move is accepted and the tickets are reduced
			System.out.println("test ---- MrX");
			if(move.toString().substring(0,6).equals("Double")){           //If a double move is chosen Mr X accepts his move in a different way (See acceptDoubleMrX)
				System.out.println("DOUBLE");
				acceptDoubleMrX(move);
				acceptMrX(((DoubleMove) move).firstMove());
				acceptMrX(((DoubleMove) move).secondMove());
				currentPlayer.removeTicket(((DoubleMove) move).firstMove().ticket());
				currentPlayer.removeTicket(((DoubleMove) move).secondMove().ticket());
			}else {
				acceptMrX(move);										   //Otherwise MrX accepts normally
				currentPlayer.removeTicket(move.ticket());
			}
		}else{
			System.out.println("test ---- Detective");
			acceptDetective(move);										  //If the player is not Mr X they will accept the move as a detective
			currentPlayer.removeTicket(move.ticket());
		}



		System.out.println("test accept currIndex ----" + currentPlayerIndex);


		if(currentPlayerIndex != 0){									  //If the current player is not the last detective, "doMove" is called on the next detective
			doMove();
		}else {
			for(Spectator s : spectators){								  //Otherwise all of the spectators are notified of the rotation being completed
				s.onRotationComplete(this);
			}
		}

	}

	@Override
	public void startRotate() {
		//currentPlayerIndex = 0;
		doMove();														  //This will call doMove for the first player which will always be mrX as currentPlayer index will be 0 at this point
	}

	public void doMove(){
		System.out.println("test currIndex ----" + currentPlayerIndex);
		ScotlandYardPlayer player = players.get(currentPlayerIndex);
		player.player().makeMove(this, player.location(), validMoves(player), requireNonNull(this));    //makeMove is called on the current player in the round. This will cause the accept callback to be called
	}

	public void acceptMrX(Move move){
		System.out.println("test currRound ----" + currentRound);
		for(Spectator s : spectators){									  //All spectators will be notified of the round starting and onMoveMade will be called for each one
			s.onRoundStarted(this, currentRound);
			s.onMoveMade(this, move);
		}
		currentRound += 1;												  //The currentRound variable is incremented to show that MrX had made a move
	}

	public void acceptDoubleMrX(Move move){
		for(Spectator s : spectators){									  //onRoundStarted does not need to be called at first when Mr X uses a double move
			s.onMoveMade(this, move);
		}
		currentRound += 1;												  //currentRound is again incremented to show that Mr X has moved
	}


	public void acceptDetective(Move move){
		for(Spectator s : spectators){
			s.onMoveMade(this, move);								  //onMoveMade is called for each spectator
		}
	}

	//return valid Moves Player can make
	private Set<Move> validMoves(ScotlandYardPlayer x) {
		if (x.colour() == BLACK) return mrXValidMoves(x);
		else return detectiveValidMoves(x);
	}


	// returns all valid Moves Detectives can make
	private Set<Move> detectiveValidMoves(ScotlandYardPlayer current) {
		Set<Move> validmoves = new HashSet<>();
		for (Edge<Integer, Transport> edge : graph.getEdgesFrom(graph.getNode(current.location()))) { // looping through the list of the different egdes of the node
			Integer nextLocation = edge.destination().value();
			Ticket t1 = fromTransport(edge.data()); //fromTransport finds the ticket for a given transport type
			TicketMove firstMove = new TicketMove(current.colour(), t1, nextLocation);

			if (isLocationEmpty(nextLocation) || players.get(0).location() == nextLocation) {
				if (current.hasTickets(t1)) {
					validmoves.add(firstMove); //if the current player has the ticket t1, then this is a valid move and is then added to the Set of valid moves
				}
			}
		}
		if (validmoves.isEmpty()) {
			validmoves.add(new PassMove(current.colour())); //if the set, validmoves, is empty then there are no valid moves hence Pass move
		}

		return Collections.unmodifiableSet(validmoves);


	}


	// returns all valid Moves MrX can make
	private Set<Move> mrXValidMoves(ScotlandYardPlayer X) {
		Set<Move> validmoves = new HashSet<>();
		for (Edge<Integer, Transport> x : graph.getEdgesFrom(graph.getNode(X.location()))) {

			Integer nextLocation = x.destination().value();
			Ticket t1 = fromTransport(x.data());
			TicketMove firstMove = new TicketMove(BLACK, t1, nextLocation);
			TicketMove firstMoveSecret = new TicketMove(BLACK, SECRET, nextLocation);
			if (isLocationEmpty(nextLocation)) {
				if (X.hasTickets(t1)) {
					validmoves.add(firstMove);
				}
				if (X.hasTickets(SECRET)) {
					validmoves.add(firstMoveSecret);
				}
			}




			// generates available Double Moves

			for (Edge<Integer, Transport> x2 : graph.getEdgesFrom(graph.getNode(nextLocation))) { //getting the edges from nextlocation after making first move
				Integer nextLocationDouble = x2.destination().value();
				Ticket t2 = fromTransport(x2.data());
				TicketMove secondMove = new TicketMove(X.colour(), t2, nextLocationDouble);
				TicketMove secondMoveSecret = new TicketMove(X.colour(), SECRET, nextLocationDouble);
				if (isLocationEmpty(nextLocationDouble) && isLocationEmpty(nextLocation) && (X.hasTickets(DOUBLE)) && (getCurrentRound() < (rounds.size() - 1))) {
					if (t2.equals(t1)) {
						if (X.tickets().get(t2) >= 2)
							validmoves.add(new DoubleMove(BLACK, firstMove, secondMove));
					}
					else if (X.hasTickets(t2) && X.hasTickets(t1)) {
						validmoves.add(new DoubleMove(BLACK, firstMove, secondMove));
					}
					if (X.hasTickets(SECRET)) {
						if (X.hasTickets(t2)) {
							validmoves.add(new DoubleMove(BLACK, firstMoveSecret, secondMove));
						}
						if (X.hasTickets(t1)) {
							validmoves.add(new DoubleMove(BLACK, firstMove, secondMoveSecret));
						}
					}
					if (X.tickets().get(SECRET) >= 2)
						validmoves.add(new DoubleMove(BLACK, firstMoveSecret, secondMoveSecret));
				}
			}
		}

		return Collections.unmodifiableSet(validmoves);

	}

	private boolean isLocationEmpty(Integer location) {
		for (ScotlandYardPlayer x : players) {
			if (location == x.location() && x.isDetective()) {
				return false;
			}
		}
		return true;
	}


	@Override
	public Collection<Spectator> getSpectators() {
//		List<Spectator> spectatorsCopy = new ArrayList<>();
//		for(Spectator sp : spectators){
//			spectatorsCopy.add(new );
//		}

		return spectators;
	}

	@Override
	public List<Colour> getPlayers() {
		List<Colour> playerColours = new ArrayList<>();
		for(ScotlandYardPlayer player : players){
			playerColours.add(player.colour());
		}
		return Collections.unmodifiableList(playerColours);
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		System.out.println("Enter get winning players ");
		Set<Colour> winner = new HashSet<>();
		

		if (noRoundsLeft() || detectivesCantMove()){
			winner.add(BLACK);
		}

		if (mrXCantMove() || mrXCaptured()){
			for(ScotlandYardPlayer x : nonmrxdetectives){
				winner.add(x.colour());
			}

		}
		System.out.println(winner.size());
		for(Colour x : winner){
			System.out.println("new");
			System.out.println(x.toString());
		}
		return Collections.unmodifiableSet(winner);

	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		if(colour == BLACK){
			return Optional.of(mrXLastLocation);
		}
		for(ScotlandYardPlayer player : players){
			if(player.colour() == colour){
				return Optional.of(player.location());
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		for(ScotlandYardPlayer player : players){    //loops through players and if the colour is the same as the colour inputted then it gets the amount of the ticket inputted.
			if(player.colour() == colour){
				return Optional.of(player.tickets().get(ticket));
			}
		}
		return Optional.empty();
	}

	@Override
	public boolean isGameOver() {
		if (detectivesCantMove() || mrXCantMove() || mrXCaptured() || noRoundsLeft()) {
			return true;

		}
		return false;

	}


	private boolean detectivesCantMove(){
		for(ScotlandYardPlayer x : players){
			for(Move m : detectiveValidMoves(x)){
				if ((m instanceof PassMove) && x.isDetective()){
					return true;

				}

			}

		}
		return false;
	}



	private boolean mrXCantMove(){
		ScotlandYardPlayer MrX = players.get(0);
		for (Move m : mrXValidMoves(MrX)){
			if (m instanceof PassMove){
				return true;
			}
		}
		return false;
	}





	private boolean mrXCaptured(){
		ScotlandYardPlayer mrX = players.get(0);
		for (ScotlandYardPlayer x : nonmrxdetectives){
			if(x.location() == mrX.location()){
				return true;
			}
		}
		return false;
	}


	private boolean noRoundsLeft() {
		if (getCurrentRound() >= rounds.size()) {
			return true;
		}
		return false;
	}




	@Override
	public Colour getCurrentPlayer() {
		return players.get(currentPlayerIndex).colour();
	}

	@Override
	public int getCurrentRound() {
		return currentRound;
	}


	@Override
	public List<Boolean> getRounds() {
		return Collections.unmodifiableList(rounds);
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		return new ImmutableGraph<>(graph);
	}


}
