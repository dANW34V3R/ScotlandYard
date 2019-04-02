package uk.ac.bris.cs.scotlandyard.model;

import java.util.*;

import ch.qos.logback.core.pattern.color.BlackCompositeConverter;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;

import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame {

	List<Boolean> rounds;
	Graph<Integer, Transport> graph;
	List<ScotlandYardPlayer> players;
	int playerIndex;
	int currentRound = 0;
	boolean isGameOver = false;
	int mrXLastLocation = 0;

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
			throw new IllegalArgumentException("Graph rounds");
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
			}else{
				playerIndex = n;
				if (!(configuration.tickets.containsKey(Ticket.valueOf("TAXI")) && configuration.tickets.containsKey(Ticket.valueOf("BUS")) && configuration.tickets.containsKey(Ticket.valueOf("UNDERGROUND")) && configuration.tickets.containsKey(Ticket.valueOf("DOUBLE")) && configuration.tickets.containsKey(Ticket.valueOf("SECRET"))))
					throw new IllegalArgumentException("MrX doesn't contain correct tickets");
			}


			players.add(new ScotlandYardPlayer(configuration.player, configuration.colour, configuration.location, configuration.tickets));
			n++;
		}
	}

	@Override
	public void registerSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void startRotate() {
		// TODO
		throw new RuntimeException("Implement me");
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
		for(ScotlandYardPlayer player : players){
			if(player.colour() == colour){
				return Optional.of(player.tickets().get(ticket));
			}
		}
		return Optional.empty();
	}

	@Override
	public boolean isGameOver() {
		return isGameOver;
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
