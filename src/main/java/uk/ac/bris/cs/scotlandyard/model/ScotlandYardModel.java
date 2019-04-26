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
	int playerIndex;
	int currentRound = 0;
	boolean isGameOver = false;
	int mrXLastLocation = 0;
	List<Spectator> spectators;
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
		Integer n = 0;

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
				playerIndex = n;
				if (!(configuration.tickets.containsKey(Ticket.valueOf("TAXI")) && configuration.tickets.containsKey(Ticket.valueOf("BUS")) && configuration.tickets.containsKey(Ticket.valueOf("UNDERGROUND")) && configuration.tickets.containsKey(Ticket.valueOf("DOUBLE")) && configuration.tickets.containsKey(Ticket.valueOf("SECRET"))))
					throw new IllegalArgumentException("MrX doesn't contain correct tickets");
			}


			players.add(new ScotlandYardPlayer(configuration.player, configuration.colour, configuration.location, configuration.tickets));
			n++;

			detectives = new ArrayList<>();
			for (ScotlandYardPlayer p : players) {
				if (p.isDetective()) detectives.add(p);
			}
		}
	}

	@Override
	public void registerSpectator(Spectator spectator) {
		if (spectator == null) {
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
	public void unregisterSpectator(Spectator spectator) {
		if (spectator == null) {
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
	public void startRotate() {
		if (isGameOver()) {
			throw new IllegalStateException("Game is over");
		}
		requestMove();
	}


	private void requestMove() {        //requests current player to make a move
		ScotlandYardPlayer p = players.get(playerIndex);
		p.player().makeMove(this, p.location(), validMoves(p), requireNonNull(this));
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

			if (isLocationEmpty(nextLocation)) {
				if (current.hasTickets(t1)) {
					validmoves.add(firstMove); //if the current player has the tickets t, then this is a valid move and is then added to the Set of valid moves
				}
			}
		}
		if (validmoves.isEmpty())
			validmoves.add(new PassMove(current.colour())); //if the set, moves, is empty then there are no valid moves hence Pass move


		return Collections.unmodifiableSet(validmoves);


	}


	// returns all valid Moves MrX can make
	private Set<Move> mrXValidMoves(ScotlandYardPlayer current) {
		Set<Move> validmoves = new HashSet<>();
		for (Edge<Integer, Transport> x : graph.getEdgesFrom(graph.getNode(current.location()))) {

			Integer nextLocation = x.destination().value();
			Ticket t1 = fromTransport(x.data());
			TicketMove firstMove = new TicketMove(current.colour(), t1, nextLocation);
			TicketMove firstMoveSecret = new TicketMove(BLACK, SECRET, nextLocation);
			if (isLocationEmpty(nextLocation)) {
				if (current.hasTickets(t1)) {
					validmoves.add(firstMove);
				}
				if (current.hasTickets(SECRET)) {
					validmoves.add(firstMoveSecret);
				}
			}




		// Double Move

		for (Edge<Integer, Transport> x2 : graph.getEdgesFrom(graph.getNode(nextLocation))) { //getting the edges from nextlocation after making first move
			Integer nextLocationDouble = x2.destination().value();
			Ticket t2 = fromTransport(x2.data());
			TicketMove secondMove = new TicketMove(current.colour(), t2, nextLocationDouble);
			TicketMove secondMoveSecret = new TicketMove(current.colour(), SECRET, nextLocationDouble);
			if (isLocationEmpty(nextLocationDouble) && isLocationEmpty(nextLocation) && (current.hasTickets(DOUBLE)) && (getCurrentRound() < (rounds.size() - 1))) {
				if (t2.equals(t1)) {
					if (current.tickets().get(t2) >= 2)
						validmoves.add(new DoubleMove(current.colour(), firstMove, secondMove));
				   }
				   else if (current.hasTickets(t2) && current.hasTickets(t1)) {
					validmoves.add(new DoubleMove(current.colour(), firstMove, secondMove));
				}
				if (current.hasTickets(SECRET)) {
					if (current.hasTickets(t2)) {
						validmoves.add(new DoubleMove(current.colour(), firstMoveSecret, secondMove));
					}
					if (current.hasTickets(t1)) {
						validmoves.add(new DoubleMove(current.colour(), firstMove, secondMoveSecret));
					}
				}
				if (current.tickets().get(SECRET) >= 2)
					validmoves.add(new DoubleMove(current.colour(), firstMoveSecret, secondMoveSecret));
			}
		}
	}

		return Collections.unmodifiableSet(validmoves);

}



	@Override
	public void accept(Move move) {
		if (move == null) {
			throw new NullPointerException("Move cannot be null");
		}
		ScotlandYardPlayer current = players.get(playerIndex);
		if (!(validMoves(current).contains(move))){
			throw new IllegalArgumentException("Move is not valid");
		}

		if (move.colour() == BLACK) {
			acceptMrX(move);
		}

		else acceptDetective(move);

	}


	public void acceptMrX(Move move){
//********
	}


	public void acceptDetective(Move move){
//**********
	}


	private boolean isLocationEmpty(Integer location) {
		for (ScotlandYardPlayer x : detectives) {  //loop through detectives because if mrX is at a location then detectives can still go to that location
			if (location == x.location()) return false;
		}
		return true;
	}


	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		throw new RuntimeException("Implement me");
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
		return Collections.emptySet();
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
		if (detectivesCantMove() || mrXCantMove() || mrXFound() || noRoundsLeft()) {
			return true;

		}
		return false;

		//return isGameOver
	}


	private boolean detectivesCantMove(){
		for(ScotlandYardPlayer x : detectives){
			for(Move m : detectiveValidMoves(x)){
				if (m instanceof PassMove){
					return true;

				}

			}

		}
		return false;
	}



	private boolean mrXCantMove(){
		return true;
	}



	private boolean mrXFound(){
		return true;
	}


	private boolean noRoundsLeft(){
		return true;
	}



	@Override
	public Colour getCurrentPlayer() {
		return players.get(playerIndex).colour();
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
